package yandex.translate.ru.yandextranslate;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LanguagesActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    final String url = "https://translate.yandex.net";
    final String translateKey = "trnsl.1.1.20170320T200542Z.9962e6bec7cd5b0a.a6fa1284db27c5c34ac3e26933e83ac8d5f44df1";

    Gson gson = new GsonBuilder().create();

    Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(url)
            .build();

    Languages intf = retrofit.create(Languages.class);

    public TextView tvLanguages;
    public ListView lvLanguages;
    public ArrayList<Language> usefull = new ArrayList<>();
    public int selected;
    int usefullCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_languages);

        tvLanguages = (TextView) findViewById(R.id.tvLanguages);
        lvLanguages = (ListView) findViewById(R.id.lvLanguages);
        lvLanguages.setOnItemClickListener(this);

        SharedPreferences usefullLanguages; //получаем недавно использованные языки
        usefullLanguages = getPreferences(MODE_PRIVATE);
        usefullCount = usefullLanguages.getInt("usefullCount", 0);

        for (int i=0; i<usefullCount; i++) {
            Language l = new Language();
            l.id = usefullLanguages.getInt("lan" + i + "_id", -1);
            l.fullName = usefullLanguages.getString("lan" + i + "_fullName", "");
            l.shortName = usefullLanguages.getString("lan" + i + "_shortName", "");
            usefull.add(i, l);
        }

        LanguagesDBHelper dbHelper = new LanguagesDBHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c=db.query("languages", null, null, null, null, null, null);

        if (!c.moveToFirst()) { //Проверка, есть ли языки в БД, или их надо загрузить. Если нет -- загружаем
            //Получаем данные от Яндекса
            Map<String, String> mapJson = new HashMap<>(); //Создаем JSON-файл для отправки запроса
            mapJson.put("key", translateKey); //Наш ключ выданный Яндексом
            mapJson.put("ui", "ru"); //Задаем язык вывода языков

            Reseiving reseiving = new Reseiving();
            reseiving.execute(mapJson);
            String langs="";
            try {
                langs = reseiving.get(); //Получаем данные из асинхронного потока с ответом Яндекса
            } catch (Exception e) {
                Log.d("Не получили", "Не получили");
            }

            if (langs != null) { //Если получили данные
                Map<String, String> map = gson.fromJson(langs, Map.class); // Переводим данные в map

                String key;
                String value;
                ContentValues cv = new ContentValues();

                for (Map.Entry entry : map.entrySet()) { //Заносим данные в БД
                    key = entry.getKey().toString();
                    value = entry.getValue().toString();
                    cv.put("fullName", value);
                    cv.put("shortName", key);
                    db.insert("languages", null, cv);
                    cv.clear();
                }
            } else {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent); //Возвращаем без данных
                Toast.makeText(this, "Необходимо подключение к интернету", Toast.LENGTH_LONG).show(); //Уведомляем пользователя о необходимости интернета
                finish();

            }
        }

        c = db.query("languages", null, null, null, null, null, null); //Получаем список языков

        ArrayList<Language> languages = new ArrayList<>();
        if (c.moveToFirst())
            languages=toArrayList(c); //Переводим из курсора в список языков

        c.close();
        db.close();

        Intent intent = getIntent();
        selected = intent.getIntExtra("selected", 1); //Получаем язык, который сейчас стоит

        if (usefull != null) { //Добавляем заголовки
            if (usefull.size() > 0)
                if (usefull.get(0).id != -2)
                    usefull.add(0, new Language(-2, "Недавно использованые языки", "used"));
        }

        languages.add(0, new Language(-2, "Все языки", "all")); //Добавляем заголовки

        if (usefull != null)
            if (usefull.size() > 0)
                languages.addAll(0, usefull);

        LanguageAdapter languageAdapter = new LanguageAdapter(this, languages, selected);

        // настраиваем список
        lvLanguages.setAdapter(languageAdapter);
    }

    public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
        Language chosen = (Language) itemClicked.getTag();

        if (chosen.id == -2) //Если нажатие было на заголовок -- ничего не происходит
            return;

        int k=-1;
        for (int i=0; i<usefull.size(); i++) { //Проверяем, есть ли выбранный язык в списке используемых
            if (usefull.get(i).id == chosen.id) {
                k=i;
                break;
            }
        }

        if (k != -1) { //Если да - удаляем его, чтобы потом добавить в начало
            usefull.remove(k);
            usefullCount--;
        }

        if (usefull.size()>0) //Если в испольуемых уже есть языки - значит на нуевом месте там заголовок, добавляем на первое
            usefull.add(1, chosen);
        else //Если языков нет -- добавляем на нулевое место выбранный язык
            usefull.add(0, chosen);

        if (usefullCount<6)
            usefullCount++;

        Intent intent = new Intent(); //Загружаем данные для возврата
        intent.putExtra("fullName", chosen.fullName);
        intent.putExtra("shortName", chosen.shortName);
        intent.putExtra("id", chosen.id);

        setResult(RESULT_OK, intent); //Возвращаем данные
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences usefullLanguages;
        usefullLanguages = getPreferences(MODE_PRIVATE); //Сохраняем данные о выбранных языках
        SharedPreferences.Editor ed = usefullLanguages.edit();

        ed.putInt("usefullCount", usefullCount);

        for (int i=0; i<usefullCount; i++) { //Сохраняем последние 5 выбранных языков в Preferences
            ed.putInt("lan" + i + "_id", usefull.get(i).id);
            ed.putString("lan" + i + "_fullName", usefull.get(i).fullName);
            ed.putString("lan" + i + "_shortName", usefull.get(i).shortName);
        }

        ed.commit();
    }

    public ArrayList<Language> toArrayList(Cursor c) {
        c.moveToFirst();
        ArrayList<Language> languages = new ArrayList<Language>();
        do {
            int id=c.getInt(c.getColumnIndex("id")); //Считываем из курсора очередной язык
            String fullName=c.getString(c.getColumnIndex("fullName"));
            String shortName=c.getString(c.getColumnIndex("shortName"));

            Language language=new Language(id, fullName, shortName);
            languages.add(language);
        } while (c.moveToNext());

        Collections.sort(languages); //Сортируем языки

        return languages;
    }

    class Reseiving extends AsyncTask<Map<String, String>, String, String> {

        @Override
        protected String doInBackground(Map<String, String>... mapJson) {
            Call<Object> call = intf.receive(mapJson[0]);
            String langs="";
            try {
                Response<Object> response = call.execute(); //Обращаемся к Яндексу за списком языков

                langs=readyResponseString(response.body().toString()); //Подготавливаем строчку к переводу в map
            }
            catch(IOException e) {
                return null;
            }

            return(langs);
        }

        private String readyResponseString(String s) { //Обрезаем из ответа ненужное
            int n=s.indexOf("langs=");
            String langs=s.substring(n+6, s.length()-1);
            langs=langs.replaceAll("=", "=\"");
            langs=langs.replaceAll(",", "\",");
            langs=langs.replaceAll("\\}", "\"\\}");
            return langs;
        }
    }

}

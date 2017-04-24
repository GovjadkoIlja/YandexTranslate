package yandex.translate.ru.yandextranslate;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher {

    EditText etText;
    TextView tvTranslation;
    Button btnInputLanguage;
    Button btnOutputLanguage;
    ImageButton btnChange;
    ImageButton btnDelete;
    ImageButton btnInFavorite;
    ImageButton btnFavorites;
    ImageButton btnHistory;
    ListView lvDictionary;
    ImageButton btnPast;

    final String translateUrl = "https://translate.yandex.net";
    final String dictionaryUrl = "https://dictionary.yandex.net";
    final String translateKey = "trnsl.1.1.20170320T200542Z.9962e6bec7cd5b0a.a6fa1284db27c5c34ac3e26933e83ac8d5f44df1";
    final String dictionaryKey = "dict.1.1.20170419T100337Z.11faf85a6b4a91e4.db65dc0b281017ea425a2d0216e7a1bde1082362";

    Language lanFrom = new Language();
    Language lanTo = new Language();
    boolean inDictionary;
    boolean fromDictionary;

    SharedPreferences savedData;
    ArrayList<DictionaryAnwser> dictionaryAnwsers = new ArrayList<>();

    String lastWord = "";
    String lastTranslation = "";
    boolean isAdded = false;
    String wordPos;
    String wordGen;
    long currentId = -1;
    int fromCash = 0; //2 - ждем двух переводом слова из кэша. 1 - одного перевода <=0 - слово не з кэша
    boolean fromDb = false; //Загружаем данные из БД, если нет интернета
    boolean internet = false;
    boolean transUpdated = false;

    Gson gson = new GsonBuilder().create();

    Retrofit translationRetrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(translateUrl)
            .build();

    YandexTranslate translationIntf = translationRetrofit.create(YandexTranslate.class);

    Retrofit dictionaryRetrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(dictionaryUrl)
            .build();

    DictionaryRecieve dictionaryIntf = dictionaryRetrofit.create(DictionaryRecieve.class);

    DictionaryTranslate dictionaryTranslationIntf = dictionaryRetrofit.create(DictionaryTranslate.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etText = (EditText) findViewById(R.id.etText);
        tvTranslation = (TextView) findViewById(R.id.tvTranslation);
        btnInputLanguage = (Button) findViewById(R.id.btnInputLanguage);
        btnOutputLanguage = (Button) findViewById(R.id.btnOuputLanguage);
        btnChange = (ImageButton) findViewById(R.id.btnChange);
        btnDelete = (ImageButton) findViewById(R.id.btnDelete);
        btnInFavorite = (ImageButton) findViewById(R.id.btnInFavorite);
        btnFavorites = (ImageButton) findViewById(R.id.btnFavorites);
        btnHistory = (ImageButton) findViewById(R.id.btnHistory);
        lvDictionary = (ListView) findViewById(R.id.lvDictionary);
        btnPast = (ImageButton) findViewById(R.id.btnPast);

        etText.addTextChangedListener(this);

        btnInputLanguage.setOnClickListener(this);
        btnOutputLanguage.setOnClickListener(this);
        btnChange.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
        btnInFavorite.setOnClickListener(this);
        btnFavorites.setOnClickListener(this);
        btnHistory.setOnClickListener(this);
        btnPast.setOnClickListener(this);

        setData(); //Восстанавливаем данные: языки и текст

        btnInputLanguage.setText(textLanguageButton(lanFrom.fullName));
        btnOutputLanguage.setText(textLanguageButton(lanTo.fullName));

        if (etText.getText().toString().trim().length() == 0) {  //Если поле текста пустое -- скрываем кнопку избранных и удаления
            btnDelete.setVisibility(View.INVISIBLE);
            btnInFavorite.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (fromCash <= 0) { //Если мы что-то дописали -- сбрасываем id кэша. И появляем кнопку назад
            currentId = -1;
            btnPast.setVisibility(View.VISIBLE);
        }

        if ((lanFrom.id == -1) && (lanTo.id == -1)) {  //Если не выбраны языки -- пишем об этом
            tvTranslation.setText("Задайте языки с которого переводите и на который переводите");
            return;
        } else if (lanTo.id == -1) {
            tvTranslation.setText("Задайте язык на который переводите");
            return;
        } else if (lanFrom.id == -1) {
            tvTranslation.setText("Задайте язык с которого переводите");
            return;
        }

        if ((lastWord.length() > s.length() + 1) && (!isAdded) && (transUpdated )) { //Если стерли два символа - значит изначально было искомое слово
            addToHistory(lastWord, lastTranslation, -1);
            lastWord = s.toString().trim();
        } else if ((lastWord.length() <= s.length()) && (!s.equals(lastWord))) { //Если просто дописали что-то - запоминаем новое слово
            lastWord = s.toString().trim();
            isAdded = false;
            transUpdated = false;
        } else if ((lastWord.length() == s.length() + 1) && (!isAdded) && (internet)) { //Если стерли в первый раз - запоминаем перевод который был
            lastTranslation = tvTranslation.getText().toString();
            transUpdated = true;
        } else if (isAdded) //Если слово уже добавлено - постоянно обновляем текущее слово, чтообы отследить, когда начнем дописывать
            lastWord = s.toString().trim();

        if (etText.getText().toString().trim().length() == 0) {    //Прячем или показываем стереть и добавить в избранные
            btnDelete.setVisibility(View.INVISIBLE);
            btnInFavorite.setVisibility(View.INVISIBLE);
        } else {
            btnDelete.setVisibility(View.VISIBLE);
        }

        if (fromCash > 0)
            btnInFavorite.setVisibility(View.VISIBLE);

        Map<String, String> mapJson = new HashMap<>(); //Создаем JSON-файл для отправки запроса
        mapJson.put("text", "\"" + etText.getText().toString() + "\""); //Переводимый текст
        mapJson.put("lang", lanFrom.shortName + "-" + lanTo.shortName); //Задаем язык с какого-на какой переводим

        fromDb = false;
        fromDictionary = false;

        if (inDictionary) {     //Идем к переводчику и к словарю вместе
            mapJson.put("key", translateKey); //Наш ключ от переводчика
            Translation transition = new Translation();
            transition.execute(mapJson);

            Map<String, String> mapJson1 = new HashMap<>();
            mapJson1.put("text", "\"" + etText.getText().toString() + "\""); //Переводимый текст
            mapJson1.put("lang", lanFrom.shortName + "-" + lanTo.shortName); //Задаем язык с какого-на какой переводим
            mapJson1.put("key", dictionaryKey); //Наш ключ от словаря
            mapJson1.put("ui", "ru");
            mapJson1.put("flags", "2");
            DictionaryTranslation dictionaryTranslation = new DictionaryTranslation();
            dictionaryTranslation.execute(mapJson1);
        } else { //Идем к переводчику
            mapJson.put("key", translateKey); //Наш ключ от переводчика
            Translation transition = new Translation();
            transition.execute(mapJson);
        }
    }

    @Override
    public void afterTextChanged(Editable s) { }

    @Override
    protected void onStop() {
        super.onStop();

        savedData.edit().clear().commit(); //Очищаем прошлые настройки

        savedData = getPreferences(MODE_PRIVATE); //Сохраняем данные о выбранных языках
        SharedPreferences.Editor ed = savedData.edit();
        ed.putInt("lanFrom_id", lanFrom.id);
        ed.putString("lanFrom_fullName", lanFrom.fullName);
        ed.putString("lanFrom_shortName", lanFrom.shortName);

        ed.putInt("lanTo_id", lanTo.id);
        ed.putString("lanTo_fullName", lanTo.fullName);
        ed.putString("lanTo_shortName", lanTo.shortName);

        ed.putString("word", etText.getText().toString());
        ed.putString("translation", tvTranslation.getText().toString());

        ed.putLong("currentId", currentId);

        ed.commit();
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.btnInputLanguage:
                intent = new Intent(this, LanguagesActivity.class);
                intent.putExtra("selected", lanFrom.id);
                startActivityForResult(intent, 0);

                break;

            case R.id.btnOuputLanguage:
                intent = new Intent(this, LanguagesActivity.class);
                intent.putExtra("selected", lanTo.id);
                startActivityForResult(intent, 1);

                break;

            case R.id.btnChange:
                Language l;
                l = lanFrom;
                lanFrom = lanTo;
                lanTo = l;
                btnInputLanguage.setText(textLanguageButton(lanFrom.fullName));
                btnOutputLanguage.setText(textLanguageButton(lanTo.fullName));

                if (!tvTranslation.getText().equals("Для перевода подключитесь к Интернету")) { //Если там не информация о том, что нужен интернет
                    etText.setText(tvTranslation.getText().toString());
                    etText.setSelection(etText.getText().length()); //Выставляем курсор в конец
                    tvTranslation.setText("");
                }

                dictionaryAnwsers.clear(); //Очищаем результаты перевода
                break;

            case R.id.btnDelete:
                addToHistory(etText.getText().toString().trim(), tvTranslation.getText().toString().trim(), -1); //При стирании -- добавляем в историю
                etText.setText("");
                break;

            case R.id.btnInFavorite: //Удаляем или добавляем в избранные
                if ((int) btnInFavorite.getTag() == 0) { //Определяем надо добавить или удалить из избранных и делаем это
                    btnInFavorite.setTag(1);
                    btnInFavorite.setImageResource(R.drawable.infavorite);
                    addToHistory(etText.getText().toString().trim(), tvTranslation.getText().toString(), 1);
                } else {
                    btnInFavorite.setTag(0);
                    btnInFavorite.setImageResource(R.drawable.nofavorite);
                    addToHistory(etText.getText().toString().trim(), tvTranslation.getText().toString(), 0);
                }

                break;

            case R.id.btnFavorites:
                if ((lastWord.trim().length() > 0) && (!isAdded) && (internet) && (fromCash <= 0)) //Если не пустая, не из кэша и есть интернет - добавляем в историю
                    addToHistory(lastWord, lastTranslation, -1);
                intent = new Intent(this, WordsActivity.class);
                intent.putExtra("isFavorites", true);
                startActivityForResult(intent, 2);
                break;

            case R.id.btnHistory:
                if ((lastWord.trim().length() > 0) && (!isAdded) && (internet) && (fromCash <= 0)) //Если не пустая, не из кэша и есть интернет - добавляем в историю
                    addToHistory(lastWord, lastTranslation, -1);
                intent = new Intent(this, WordsActivity.class);
                intent.putExtra("isFavorites", false);
                startActivityForResult(intent, 2);
                break;

            case R.id.btnPast:
                WordsDBHelper dbHelper = new WordsDBHelper(this);
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                Cursor c;

                if (currentId == -1) {  //Если уже искали ID - ищем максимальный, меньше его. Если не искали - ищем максимальный
                    c = db.query("words", new String[] { "MAX(id)" }, null, null, null, null, null);
                } else
                    c = db.query("words", new String[] { "MAX(id)" }, "id < ?", new String[] { Long.toString(currentId) }, null, null, null);


                if (c.moveToFirst())
                    currentId = c.getInt(c.getColumnIndex("MAX(id)"));

                if (currentId == 0) { //Дошли до последнего элемента истории
                    btnPast.setVisibility(View.INVISIBLE);
                    return;
                }

                dictionaryAnwsers.clear();

                c = db.query("words", null, "id = ?", new String[] { Long.toString(currentId) }, null, null, null);

                if (c.moveToFirst()) {
                    int lanFromId;
                    int lanToId;
                    lanFromId = c.getInt(c.getColumnIndex("lanFromId"));
                    lanToId = c.getInt(c.getColumnIndex("lanToId"));

                    if ((lanFromId != lanTo.id) || (lanToId != lanFrom.id)) { //Если языки поменялись
                        if ((lanFromId == lanTo.id) && (lanToId == lanFrom.id)) //Если языки стоят просто наоборот
                            btnChange.callOnClick();
                        else if (lanFromId == lanTo.id) {
                            lanFrom.id = lanTo.id;
                            lanFrom.shortName = lanTo.shortName;
                            lanFrom.fullName = lanTo.fullName;
                            btnInputLanguage.setText(textLanguageButton(lanFrom.fullName));

                            lanTo = lanById(lanToId);
                            btnOutputLanguage.setText(textLanguageButton(lanTo.fullName));
                        } else if (lanToId == lanFrom.id) {
                            lanTo.id = lanFrom.id;
                            lanTo.fullName = lanFrom.fullName;
                            lanTo.shortName = lanFrom.shortName;
                            btnOutputLanguage.setText(textLanguageButton(lanTo.fullName));

                            lanFrom = lanById(lanFromId);
                            btnInputLanguage.setText(textLanguageButton(lanFrom.fullName));
                        } else {   //Если оба языка не совпадают
                            lanFrom = lanById(lanFromId);
                            btnInputLanguage.setText(textLanguageButton(lanFrom.fullName));
                            lanTo = lanById(lanToId);
                            btnOutputLanguage.setText(textLanguageButton(lanTo.fullName));
                        }

                        checkInDictionary();
                    }

                }

                tvTranslation.setText(c.getString(c.getColumnIndex("translation")));

                if (inDictionary) //Если из словаря, то нужно два перевода
                    fromCash = 2;
                else
                    fromCash = 1;
                etText.setText(c.getString(c.getColumnIndex("word")));
                etText.setSelection(etText.getText().length()); //Выставляем курсор в конец

                if (c.getInt(c.getColumnIndex("favorite")) == 1) {
                    btnInFavorite.setImageResource(R.drawable.infavorite);
                    btnInFavorite.setTag(1);
                }
                else {
                    btnInFavorite.setImageResource(R.drawable.nofavorite);
                    btnInFavorite.setTag(0);
                }
                c.close();
                db.close();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:  //Если вернулось из входного языка
                if (data == null) { //Если язык не был выбран - ничего не меняем
                    return;
                }

                if ((data.getIntExtra("id", -1) != -1) && (data.getIntExtra("id", -1) != lanFrom.id)) { //Если получили какие-то данные и язык новый
                    dictionaryAnwsers.clear(); //Очищаем результаты перевода
                    lanFrom.id = data.getIntExtra("id", -1);
                    lanFrom.fullName = data.getStringExtra("fullName");
                    lanFrom.shortName = data.getStringExtra("shortName");
                    btnInputLanguage.setText(textLanguageButton(lanFrom.fullName));

                    checkInDictionary();
                    onTextChanged(etText.getText().toString(), 0, 1, etText.getText().toString().length()); //Обновляем перевод, так как у нас новый язык
                }
                break;

            case 1:  //Если вернулось из выходного языка
                if (data == null) {  //Если язык не был выбран - ничего не меняем
                    return;
                }

                if ((data.getIntExtra("id", -1) != -1)  && (data.getIntExtra("id", -1) != lanTo.id)) { //Если получили какие-то данные и язык новый
                    dictionaryAnwsers.clear(); //Очищаем результаты перевода
                    lanTo.fullName = data.getStringExtra("fullName");
                    lanTo.shortName = data.getStringExtra("shortName");
                    lanTo.id = data.getIntExtra("id", 0);
                    btnOutputLanguage.setText(textLanguageButton(lanTo.fullName));

                    checkInDictionary();
                    onTextChanged(etText.getText().toString(), 0, 1, etText.getText().toString().length()); //Обновляем перевод
                }

                break;

            case 2:  //Если вернулось от истории или избранного
                if (data == null) //Если не ввернулось ничего
                    return;

                if (data.getIntExtra("id", -1) == -1)  //Если не получили id слова
                    return;

                dictionaryAnwsers.clear(); //Очищаем результаты прошлого перевода
                int lanFromId = data.getIntExtra("lanFromId", -1); //Получаем языки слова из истории
                int lanToId = data.getIntExtra("lanToId", -1);

                LanguagesDBHelper dbHelper = new LanguagesDBHelper(this);
                SQLiteDatabase db = dbHelper.getWritableDatabase();

                Cursor c;

                c=db.query("languages", null, "id = ?", new String[]{Integer.toString(lanFromId)}, null, null, null);

                if (c.moveToFirst()) { //Если есть язык ввода - устанавливаем его
                    lanFrom.id = lanFromId;
                    lanFrom.fullName=c.getString(c.getColumnIndex("fullName"));
                    lanFrom.shortName=c.getString(c.getColumnIndex("shortName"));

                    btnInputLanguage.setText(textLanguageButton(lanFrom.fullName));
                }

                c=db.query("languages", null, "id = ?", new String[]{Integer.toString(lanToId)}, null, null, null);

                if (c.moveToFirst()) {  //Если есть язык вывода - устанавливаем его
                    lanTo.id = lanToId;
                    lanTo.fullName=c.getString(c.getColumnIndex("fullName"));
                    lanTo.shortName=c.getString(c.getColumnIndex("shortName"));

                    btnOutputLanguage.setText(textLanguageButton(lanTo.fullName));
                }

                checkInDictionary(); //Проверем в словаре ли наши языки

                etText.setText(data.getStringExtra("word")); //Обновляем текст

                c.close();
                db.close();
                break;
        }

        etText.setSelection(etText.getText().length()); //Выставляем курсор в конец слова

        //onTextChanged(etText.getText().toString(), 0, 1, etText.getText().toString().length());
    }

    public void setData() {     //Выставляет данные при начале работы с активити
        savedData = getPreferences(MODE_PRIVATE);
        lanFrom.id = savedData.getInt("lanFrom_id", -1);
        lanFrom.fullName = savedData.getString("lanFrom_fullName", "Выберите язык");
        lanFrom.shortName = savedData.getString("lanFrom_shortName", "none");

        lanTo.id = savedData.getInt("lanTo_id", -1);
        lanTo.fullName = savedData.getString("lanTo_fullName", "Выберите язык");
        lanTo.shortName = savedData.getString("lanTo_shortName", "none");

        checkInDictionary();
        tvTranslation.setText(savedData.getString("translation", ""));
        String word = savedData.getString("word", "");

        if (word.trim().length() > 0) {
            if (inDictionary)
                fromCash = 2;
            else
                fromCash = 1;
        }


        etText.setText(word);
        checkInFavorite(word);

        etText.setSelection(etText.getText().length()); //Выставляем курсор в конец

        currentId = savedData.getLong("currentId", -1);

    }

    public Language lanById(int id) { //Ищем в БД язык по его ID
        LanguagesDBHelper dbHelper = new LanguagesDBHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c;

        c = db.query("languages", null, "id = ?", new String[] { Integer.toString(id) }, null, null, null);

        Language lan = new Language();

        if (c.moveToFirst()) {
            lan.id = id;
            lan.fullName = c.getString(c.getColumnIndex("fullName"));
            lan.shortName = c.getString(c.getColumnIndex("shortName"));
        }

        c.close();
        db.close();

        return lan;
    }

    public void checkInDictionary() { //Проверка в словаре языки или нет
        DictionaryDBHelper dictionaryDBHelper = new DictionaryDBHelper(this);
        SQLiteDatabase db = dictionaryDBHelper.getWritableDatabase();

        Cursor c=db.query("dictionary", null, null, null, null, null, null);
        if (!c.moveToFirst()) {  //Если таблица языков словаря пустая - получаем языки
            ArrayList<String> dictionaryLanguages = new ArrayList<String>();

            Map<String, String> mapJson = new HashMap<String, String>(); //Создаем JSON-файл для отправки запроса
            mapJson.put("key", dictionaryKey); //Наш ключ выданный Яндексом

            DictionaryReseiving reseiving = new DictionaryReseiving();
            reseiving.execute(mapJson);

            try {
                dictionaryLanguages = reseiving.get();
            } catch (Exception e) {
                Log.d("Не получили", "Не получили");
            }

            if (dictionaryLanguages != null) { //Проверяем - смогли ли получить языки
                String pair;
                for (int i = 0; i < dictionaryLanguages.size(); i++) { //Заполняем таблицу
                    pair = dictionaryLanguages.get(i);
                    ContentValues cv = new ContentValues();

                    cv.put("lanFrom", pair.split("-")[0]);
                    cv.put("lanTo", pair.split("-")[1]);

                    db.insert("dictionary", null, cv);
                    cv.clear();
                }
            }
        }

        c=db.query("dictionary", null, "lanFrom = ? AND lanTo = ?", new String[] {lanFrom.shortName, lanTo.shortName}, null, null, null);

        inDictionary=c.moveToFirst();

        c.close();
        db.close();
    }

    public void addToHistory(String word, String translation, int favorite) {  //Добавление лова в историю
        ContentValues cv = new ContentValues();  //Заполняем значения для добавления в БД
        cv.put("word", word);
        cv.put("translation", translation);
        cv.put("lanFromId", lanFrom.id);
        cv.put("lanFrom", lanFrom.shortName);
        cv.put("lanToId", lanTo.id);
        cv.put("lanTo", lanTo.shortName);

        WordsDBHelper dbHelper = new WordsDBHelper(MainActivity.this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c = db.query("words", null, "word = ? AND lanFromId = ? AND lanToId = ?", new String[]{word, Integer.toString(lanFrom.id), Integer.toString(lanTo.id) }, null, null, null);  //Выбираем слова из истории такие же, как наше

        if (c.moveToFirst()) { //Проверяем есть ли в истории наше слово
            if (favorite == -1) //Если не передали в иззбранных будет наше слово или нет
                favorite = c.getInt(c.getColumnIndex("favorite"));
            cv.put("favorite", favorite);

            int id = c.getInt(c.getColumnIndex("id"));
            db.delete("words", "id = " + id, null); //Удаляем из БД переводимое слово, если оно там уже есть
        }
        cv.put("favorite", favorite);

        db.insert("words", null, cv); //Добавляем в БД новое слово

        c.close();
        db.close();
        isAdded = true; //Помечаем, что данное слово мы добавили
    }

    public void checkInFavorite(String word) {  //Проверем в избранных слово или нет и выставляем кнопку
        WordsDBHelper dbHelper = new WordsDBHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c = db.query("words", null, "word = ? AND lanFromId = ? AND lanToId = ?", new String[]{word, Integer.toString(lanFrom.id), Integer.toString(lanTo.id)}, null, null, null);

        if (c.moveToFirst()) { //Проверяем, есть ли в истории это слово
            int favorite = c.getInt(c.getColumnIndex("favorite"));

            if (favorite == 1) {
                btnInFavorite.setTag(1);
                btnInFavorite.setImageResource(R.drawable.infavorite);
            } else {
                btnInFavorite.setTag(0);
                btnInFavorite.setImageResource(R.drawable.nofavorite);
            }
        } else {
            btnInFavorite.setTag(0);
            btnInFavorite.setImageResource(R.drawable.nofavorite);
        }

        c.close();
        db.close();
    }

    public String textLanguageButton(String language) { //Выставляем правильный текст на кнопкт языков
        if (language.length()>12) //Вмещается не более 12 букв
            if (language.charAt(10) != ' ') //Если последний оставляемый символ пробел -- обрезаем до предпоследнего
                language=language.substring(0, 11) + "...";
            else
                language=language.substring(0, 10) + "...";
        return language;
    }

    class Translation extends AsyncTask<Map<String, String>, Void, String> { //Перевод через переводчик

        @Override
        protected String doInBackground(Map<String, String>... mapJson) {

            boolean cash = (fromCash > 0); //Чтобы во время перевода могли менять fromCash в других потоках
            fromCash--;

            Call<Object> call = translationIntf.translate(mapJson[0]);

            try {
                Response<Object> response = call.execute(); //Поолучаем данные от сервера

                String anwser=response.body().toString(); //Преобразуем response, которых местах кавычки в нем стоят неправильно. Например ru-en слово пира
                anwser=anwser.replaceAll("\"", "");
                anwser = anwser.replaceAll("\\[", "[\"");
                anwser = anwser.replaceAll("]", "\"]");

                Map<String, String> map = gson.fromJson(anwser, Map.class); //Преобразуем строку в Map
                internet = true; //Если результат получили, значит интернет есть
                for (Map.Entry e : map.entrySet()) { //Ищем элемент с переводом
                    if (e.getKey().equals("text")) {
                        return (e.getValue().toString());
                    }
                }

            } catch (IOException e) { //Не удалось перевести
                internet = false;
                if (cash) //Если пришли из кэша не пишем об интернете, а ничего не меняем
                    return " ";
                return null;
            }
            return null;
        }


        @Override
        protected void onPostExecute(String trans) {
            if (trans == null) {
                noInternet();
                return;
            }

            if ((trans.length() > 2) && (etText.getText().toString().trim().length() > 0))  //Если не только квадратные скобки
                btnInFavorite.setVisibility(View.VISIBLE);

            if (trans.equals(" ")) //Пришли из кэша
                return;

            if (fromDictionary) //Если перевод этого слова с помощью словаря уже есть
                return;

            String translation = trans.substring(1, trans.length() - 1); // Обрезаем квадратные скобки
            tvTranslation.setText(translation.trim());

            if (etText.length() == lastWord.length())
                lastTranslation = translation;

            dictionaryAnwsers.clear(); //Убиваем результаты прошлого ответа словаря

            checkInFavorite(etText.getText().toString().trim());
        }
    }

    class DictionaryReseiving extends AsyncTask<Map<String, String>, String, ArrayList<String>> { //Получение списка языков от словаря

        @Override
        protected ArrayList<String> doInBackground(Map<String, String>... mapJson) {
            Call<Object> call = dictionaryIntf.receive(mapJson[0]);
            String langsString = "";

            try {
                Response<Object> response = call.execute(); //Обращаемся к Яндексу за списком языков
                langsString = response.body().toString();
            } catch (IOException e) {
                e.printStackTrace();
            }

            ArrayList<String> langsList = gson.fromJson(langsString, ArrayList.class); //Преобразуем ответ в ArrayList
            return langsList;
        }
    }

    class DictionaryTranslation extends AsyncTask<Map<String, String>, Void, String> { //Перевод с помощью словаря

        @Override
        protected String doInBackground(Map<String, String>... mapJson) {
            boolean cash = (fromCash > 0); //Чтобы во время перевода могли менять fromCash
            fromCash--;

            Call<Object> call = dictionaryTranslationIntf.translate(mapJson[0]);

            String anwser="";
            try {
                Response<Object> response = call.execute(); //Обращаемся к Яндексу за переводом

                anwser = response.body().toString();

            } catch (IOException e) {
                internet = false; //Если ошика -- интернета нет
                if (cash) //Если пришли из кэша не пишем об интернете, а ничего не меняем
                    return " ";
                return null;
            }

            internet = true;
            return anwser;
        }

        @Override
        protected void onPostExecute(String anwser) {
            if (anwser == null) { //Если нет результата
                noInternet();
                dictionaryAnwsers.clear();
                return;
            }

            if (anwser.equals(" "))
                return;

            if (anwser.length() > 17) { //Если словарь знает это слово
                btnInFavorite.setVisibility(View.VISIBLE);

                fromDictionary = true;
                parseAnwser(anwser);

                tvTranslation.setText(dictionaryAnwsers.get(0).elements.get(0).trim());
                lastTranslation = dictionaryAnwsers.get(0).elements.get(0);

                checkInFavorite(etText.getText().toString().trim());

                DictionaryAdapter dictionaryAdapter = new DictionaryAdapter(MainActivity.this, dictionaryAnwsers); //Задаем адаптер для списка значений
                // настраиваем список
                lvDictionary.setAdapter(dictionaryAdapter);
            }
        }
    }

    public static int bracketsCounter(String s, boolean square) { //Подсчет скобок. Возвращает - номер элемента строки, когда первая скобка закрывается
        int counter=0;

        char a, b;
        if (square) { //Если считаем квадратные скобки
            a='[';
            b=']';
        } else {
            a='{';
            b='}';
        }

        for (int i=0; i<s.length(); i++) { //Если считаем круглые скобки
            if (s.charAt(i) == a)
                counter++;
            if (s.charAt(i) == b) {
                counter--;
                if (counter == 0)
                    return ++i;
            }
        }

        return -1;
    }

    public void parseAnwser(String anwser) { //Парсим ответ словря
        dictionaryAnwsers.clear();

        int last = anwser.indexOf("tr=[");
        String head = anwser.substring(0, last);

        int pointer = head.indexOf("pos=")+4;
        last = head.indexOf(',', pointer);
        wordPos = head.substring(pointer, last);  //Часть речи слова

        pointer = head.indexOf("gen=", last)+4;

        if (pointer != 3) {
            last = head.indexOf(',', pointer);
            wordGen = head.substring(pointer, last); //Род слова
        }

        pointer = anwser.indexOf("def=[")+5;
        anwser = anwser.substring(pointer);

        String definition; //Разбиваем по значениям

        last=0;
        while ((last<anwser.length()-4) && (pointer != -1)) {
            anwser = anwser.substring(last); //Обрезаем строку до следующего значения

            last = bracketsCounter(anwser, false);
            definition = anwser.substring(0, last);

            pointer = definition.indexOf("tr=[")+4;

            definition = definition.substring(pointer);

            int pointerDef = 0;
            while (pointerDef < definition.length()-4) {
                definition = definition.substring(pointerDef);
                pointerDef = bracketsCounter(definition, false); //Выделяем текущее подзначение

                DictionaryAnwser s = new DictionaryAnwser(definition.substring(0, pointerDef));

                dictionaryAnwsers.add(s);
            }
        }
    }

    public void noInternet() { //Если нет интернета пытаемся получить из БД наше слово
        if (!fromDb) { //Если нет интернета -- пытаемся найти это слово в БД
            fromDb = true; //Пишем, что уже ищем в БД, чтобы в другом потоке не делать то же самое
            WordsDBHelper dbHelper = new WordsDBHelper(MainActivity.this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor c;

            c = db.query("words", null, "word = ? AND lanFromId = ? AND lanToId = ?", new String[] { etText.getText().toString(), Integer.toString(lanFrom.id), Integer.toString(lanTo.id) }, null, null, null); //Ищем введенное слово в БД

            if (c.moveToFirst()) {
                if (!internet) { //Если второй поток либо еще не перевел, либо тоже не смог подключиться - выводим результат из БД
                    tvTranslation.setText(c.getString(c.getColumnIndex("translation")));
                    if (c.getInt(c.getColumnIndex("favorite")) == 1) {
                        btnInFavorite.setTag(1);
                        btnInFavorite.setImageResource(R.drawable.infavorite);
                    } else {
                        btnInFavorite.setTag(0);
                        btnInFavorite.setImageResource(R.drawable.nofavorite);
                    }

                    addToHistory(etText.getText().toString(), tvTranslation.getText().toString(), (int)btnInFavorite.getTag()); //Поднимаем наверх истории
                }
                btnInFavorite.setVisibility(View.VISIBLE);
                c.close();
                db.close();
                return;
            }

            tvTranslation.setText("Для перевода подключитесь к Интернету"); //Если не получилось найти в БД пишем, что нужен интернет
            btnInFavorite.setVisibility(View.INVISIBLE); //Скрываем кнопку добавления в избранные
        }

        return;
    }
}
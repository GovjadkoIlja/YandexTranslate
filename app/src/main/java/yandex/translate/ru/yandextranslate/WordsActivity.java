package yandex.translate.ru.yandextranslate;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class WordsActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    TextView tvWord;
    TextView tvTranslation;
    TextView tvLanguages;
    ListView lvWords;
    EditText etSearch;
    ImageButton btnMain;
    ImageButton btnFavorites;
    ImageButton btnHistory;
    ImageButton btnErase;
    ImageButton btnDeleteText;
    RelativeLayout layoutNoFavorites;
    RelativeLayout layoutNoHistory;

    ArrayList<Word> words;
    boolean isFavorites; //надо вывести избранные или все слова

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_words);

        tvWord = (TextView) findViewById(R.id.tvWord);
        tvTranslation = (TextView) findViewById(R.id.tvTranslation);
        tvLanguages = (TextView) findViewById(R.id.tvLanguages);
        etSearch = (EditText) findViewById(R.id.etSearch);
        btnMain = (ImageButton) findViewById(R.id.btnMain);
        btnFavorites = (ImageButton) findViewById(R.id.btnFavorites);
        btnHistory = (ImageButton) findViewById(R.id.btnHistory);
        btnErase = (ImageButton) findViewById(R.id.btnErase);
        btnDeleteText = (ImageButton) findViewById(R.id.btnDeleteText);
        lvWords = (ListView) findViewById(R.id.lvWords);
        layoutNoFavorites = (RelativeLayout) findViewById(R.id.layoutNoFavorites);
        layoutNoHistory = (RelativeLayout) findViewById(R.id.layoutNoHistory);

        lvWords.setOnItemClickListener(this);
        btnMain.setOnClickListener(this);
        btnFavorites.setOnClickListener(this);
        btnHistory.setOnClickListener(this);
        btnErase.setOnClickListener(this);
        btnDeleteText.setOnClickListener(this);

        etSearch.addTextChangedListener(new TextWatcher() { //Делаем динамический поиск
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0)
                    btnDeleteText.setVisibility(View.GONE);
                else
                    btnDeleteText.setVisibility(View.VISIBLE);

                ArrayList<Word> searchResult = new ArrayList<>();
                Word w;
                for (int i=0; i<words.size(); i++) {
                    w = words.get(i);
                    if (w.word.startsWith(s.toString()) || (w.translation.startsWith(s.toString()))) //Если элемент начинается со строки поиска -- выводим его
                        searchResult.add(w);
                }

                WordAdapter wordAdapter = new WordAdapter(WordsActivity.this, searchResult); //Задаем адаптер для списка результатов поиска

                lvWords.setAdapter(wordAdapter); // настраиваем список
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        WordsDBHelper dbHelper = new WordsDBHelper(this);  // Объект для создания и управления БД
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c;

        Intent intent = getIntent();
        isFavorites = intent.getBooleanExtra("isFavorites", false); //Получаем, избранные мы выводим или нет

        if (isFavorites) { //Ищем слова, которые надо выводить
            btnFavorites.setImageResource(R.drawable.enablefavorite);
            c = db.query("words", null, "favorite = ?", new String[]{"1"}, null, null, null);
        } else {
            btnHistory.setImageResource(R.drawable.enablehistory);
            c = db.query("words", null, null, null, null, null, null);
        }

        words = toArrayList(c); //Переводим из курсора в список языков

        c.close();
        db.close();

        if (words.size() == 0) {
            btnErase.setVisibility(View.INVISIBLE); //Скрываем кнопку очистки
            if (isFavorites)
                layoutNoFavorites.setVisibility(View.VISIBLE);
            else
                layoutNoHistory.setVisibility(View.VISIBLE);
        }

        WordAdapter wordAdapter = new WordAdapter(this, words); //Задаем адаптер для списка

        // настраиваем список
        lvWords.setAdapter(wordAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.btnMain):
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;

            case (R.id.btnFavorites): //Если теперь хоим вывести избранные
                if (!isFavorites) { //Если раньше выводили историю
                    layoutNoHistory.setVisibility(View.GONE);
                    WordsDBHelper dbHelper = new WordsDBHelper(this);
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    Cursor c;
                    isFavorites = true;
                    btnFavorites.setImageResource(R.drawable.enablefavorite); //Перерисовываем иконки
                    btnHistory.setImageResource(R.drawable.disablehistory);
                    c = db.query("words", null, "favorite = ?", new String[]{"1"}, null, null, null);

                    words = toArrayList(c); //Переводим из курсора в список слов

                    c.close();
                    db.close();

                    if (words.size() == 0) {
                        btnErase.setVisibility(View.INVISIBLE); //Скрываем кнопку очистки
                        layoutNoFavorites.setVisibility(View.VISIBLE); //Скрываем картинку, что нет истории
                    } else {
                        btnErase.setVisibility(View.VISIBLE);
                        layoutNoFavorites.setVisibility(View.GONE); //Скрываем картинку, что нет истории
                    }

                    WordAdapter wordAdapter = new WordAdapter(this, words);
                    lvWords.setAdapter(wordAdapter); // настраиваем список
                }
                break;

            case (R.id.btnHistory) : //Если теперь хотим вывести историю
                if (isFavorites) { //Если раньше выводили избранные
                    layoutNoFavorites.setVisibility(View.GONE);

                    isFavorites = false;
                    WordsDBHelper dbHelper = new WordsDBHelper(this);
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    Cursor c;

                    btnFavorites.setImageResource(R.drawable.disablefavorite); //Перерисовываем иконки
                    btnHistory.setImageResource(R.drawable.enablehistory);
                    c = db.query("words", null, null, null, null, null, null);

                    words = toArrayList(c); //Переводим из курсора в список слов

                    c.close();
                    db.close();

                    if (words.size() == 0) {
                        btnErase.setVisibility(View.INVISIBLE); //Скрываем кнопку очистки
                        layoutNoHistory.setVisibility(View.VISIBLE); //Показываем картинку, что нет истории
                    } else {
                        btnErase.setVisibility(View.VISIBLE);
                        layoutNoHistory.setVisibility(View.GONE); //Скрываем картинку, что нет истории
                    }

                    WordAdapter wordAdapter = new WordAdapter(this, words); //Задаем адаптер для списка
                    lvWords.setAdapter(wordAdapter); // настраиваем список
                }
                break;

            case (R.id.btnErase): //Если хотим удалить все данные
                openDeleteDialog();
                break;

            case (R.id.btnDeleteText):
                etSearch.setText("");
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
        Intent intent = new Intent(); //Загружаем данные для возврата
        intent.putExtra("id", position);
        intent.putExtra("word", words.get(position).word);
        intent.putExtra("lanFromId", words.get(position).lanFromId);
        intent.putExtra("lanToId", words.get(position).lanToId);
        setResult(RESULT_OK, intent); //Возвращаем данные
        finish();
    }

    public ArrayList<Word> toArrayList(Cursor c) { //Преводим курсор в ArrayList слов
        ArrayList<Word> words = new ArrayList<>();
        if (c.moveToFirst()) {
            int id;
            String word;
            String translation;
            int lanFromId;
            String lanFrom;
            int lanToId;
            String lanTo;
            boolean favorite;
            Word w;
            do {
                id = c.getInt(c.getColumnIndex("id")); //Считываем из курсора очередной язык
                word = c.getString(c.getColumnIndex("word"));
                translation = c.getString(c.getColumnIndex("translation"));
                lanFromId = c.getInt(c.getColumnIndex("lanFromId"));
                lanFrom = c.getString(c.getColumnIndex("lanFrom"));
                lanToId = c.getInt(c.getColumnIndex("lanToId"));
                lanTo = c.getString(c.getColumnIndex("lanTo"));
                favorite = (c.getInt(c.getColumnIndex("favorite")) == 1);

                w = new Word(id, word, translation, lanFromId, lanFrom, lanToId, lanTo, favorite);

                words.add(0, w); //Добавляем новое слово в список слов
            } while (c.moveToNext());
        }

        return words;
    }

    private void openDeleteDialog() {
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(this);

        if (isFavorites) //В зависимости от того где мы -- разные заголовки диалогового окна
            quitDialog.setTitle("Вы уверенны, что хотите отчистить все избранные?");
        else
            quitDialog.setTitle("Вы уверенны, что хотите отчистить всю историю?");

        quitDialog.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                WordsDBHelper dbHelper = new WordsDBHelper(WordsActivity.this);
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                Cursor c;

                ContentValues cv = new ContentValues();

                if (isFavorites) { //Если мы в избранных
                    c = db.query("words", null, "favorite=1", null, null, null, null); //Ищем все избранные слова

                    if (c.moveToFirst()) {
                        int id;
                        do { //Заносим все старые данные и то, что эти слова теперь не в избранных
                            id = c.getInt(c.getColumnIndex("id"));
                            cv.put("id", id);
                            cv.put("word", c.getString(c.getColumnIndex("word")));
                            cv.put("translation", c.getString(c.getColumnIndex("translation")));
                            cv.put("lanFromId", c.getInt(c.getColumnIndex("lanFromId")));
                            cv.put("lanFrom", c.getString(c.getColumnIndex("lanFrom")));
                            cv.put("lanFromId", c.getInt(c.getColumnIndex("lanFromId")));
                            cv.put("lanTo", c.getString(c.getColumnIndex("lanTo")));
                            cv.put("lanToId", c.getInt(c.getColumnIndex("lanToId")));
                            cv.put("favorite", 0);

                            db.update("words", cv, "id = ?", new String[] {Integer.toString(id)}); //Добавляем в БД
                            cv.clear();
                        } while (c.moveToNext());
                    }
                    layoutNoFavorites.setVisibility(View.VISIBLE); //Показываем картинку, что нет избранных

                    c.close();
                } else { //Если мы в истории
                    db.delete("words", null, null);
                    layoutNoHistory.setVisibility(View.VISIBLE); //Показываем картинку, что нет истории
                }
                db.close();

                btnErase.setVisibility(View.INVISIBLE); //Скрываем кнопку очистки

                WordAdapter wordAdapter = new WordAdapter(WordsActivity.this, new ArrayList<Word>()); //Выводим получившийся пустой список
                lvWords.setAdapter(wordAdapter); // настраиваем список
            }
        });

        quitDialog.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
            }
        });

        quitDialog.show();
    }
}

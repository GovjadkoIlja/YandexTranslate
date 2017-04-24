package yandex.translate.ru.yandextranslate;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WordsDBHelper extends SQLiteOpenHelper { //Таблица для хранения слов (избранное и история)

    public WordsDBHelper(Context context) {
        super(context, "myDB1", null, 1);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table words (" //создаем таблицу языков
                + "id integer primary key autoincrement,"
                + "word text,"
                + "translation text,"
                + "lanFromId int,"
                + "lanFrom text,"
                + "lanToId int,"
                + "lanTo text,"
                + "favorite int"
                +");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}

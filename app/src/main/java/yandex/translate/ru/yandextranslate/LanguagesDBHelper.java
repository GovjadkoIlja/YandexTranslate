package yandex.translate.ru.yandextranslate;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LanguagesDBHelper extends SQLiteOpenHelper { //Таблица для хранения доступных языков

    public LanguagesDBHelper(Context context) {
        super(context, "myDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table languages (" //создаем таблицу языков
                + "id integer primary key autoincrement,"
                + "fullName text,"
                + "shortName text"
                +");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
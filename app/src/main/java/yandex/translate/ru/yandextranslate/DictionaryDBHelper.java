package yandex.translate.ru.yandextranslate;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DictionaryDBHelper extends SQLiteOpenHelper { //Таблица для хранения языков доступных словарю

    public DictionaryDBHelper(Context context) {
        super(context, "myDB2", null, 1);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table dictionary ("
                + "id integer primary key autoincrement,"
                + "lanFrom text,"
                + "lanTo text"
                +");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}

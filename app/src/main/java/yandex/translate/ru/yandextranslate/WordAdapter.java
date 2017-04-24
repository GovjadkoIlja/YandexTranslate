package yandex.translate.ru.yandextranslate;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Илья on 08.04.2017.
 */

public class WordAdapter extends BaseAdapter  {
    Context ctx;
    LayoutInflater lInflater;
    ArrayList<Word> words;

    TextView tvWord;
    TextView tvLanguages;
    TextView tvTranslation;
    ImageButton btnFavorite;

    WordAdapter(Context context, ArrayList<Word> _words) {
        ctx = context;
        words = _words;
        lInflater = (LayoutInflater) ctx
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // кол-во элементов
    @Override
    public int getCount() {
        return words.size();
    }

    // элемент по позиции
    @Override
    public Object getItem(int position) {
        return words.get(position);
    }

    // id по позиции
    @Override
    public long getItemId(int position) {
        return position;
    }

    // пункт списка
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = lInflater.inflate(R.layout.word_item, parent, false);
        }

        Word word = (Word)getItem(position);
        tvWord = (TextView) view.findViewById(R.id.tvWord);
        tvLanguages = (TextView) view.findViewById(R.id.tvLanguages);
        tvTranslation = (TextView) view.findViewById(R.id.tvTranslation);
        btnFavorite = (ImageButton) view.findViewById(R.id.btnFavorite);
        btnFavorite.setFocusable(false); //Снимаем фокус с ImageButton

        btnFavorite.setTag(word);

        btnFavorite.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ImageButton clicked = (ImageButton)v;
                Word word = (Word) v.getTag();

                WordsDBHelper dbHelper = new WordsDBHelper(lInflater.getContext());  // объект для создания и управления версиями БД
                SQLiteDatabase db = dbHelper.getWritableDatabase();

                ContentValues cv = new ContentValues();
                cv.put("id", word.id);
                cv.put("word", word.word);
                cv.put("translation", word.translation);
                cv.put("lanFromId", word.lanFromId);
                cv.put("lanFrom", word.langFrom);
                cv.put("lanToId", word.lanToId);
                cv.put("lanTo", word.langTo);

                if (word.favorite) { //Проверяем, избранное ли это слово
                    clicked.setImageResource(R.drawable.nofavorite);
                    word.favorite = false;
                    cv.put("favorite", 0);
                }
                else {
                    clicked.setImageResource(R.drawable.infavorite);
                    word.favorite = true;
                    cv.put("favorite", 1);
                }

                db.update("words", cv, "id = ?", new String[] {Integer.toString(word.id)}); //Заносим изменения
                db.close();
            }
        });

        if (word.word.length() > 22) //Обрезаем слово
            if (word.word.charAt(19) != ' ') //Если в конце пробел -- обрезаем со следующего
                tvWord.setText(word.word.substring(0, 20) + "...");
            else
                tvWord.setText(word.word.substring(0, 19) + "...");
        else
            tvWord.setText(word.word);

        if (word.translation.length() > 26) //Обрезаем перевод
            if (word.word.charAt(23) != ' ') //Если в конце пробел -- обрезаем со следующего
                tvTranslation.setText(word.translation.substring(0, 24) + "...");
            else
                tvTranslation.setText(word.translation.substring(0, 23) + "...");
        else
            tvTranslation.setText(word.translation);

        tvLanguages.setText(word.langFrom.toUpperCase()+"-"+word.langTo.toUpperCase());
        if (word.favorite)
            btnFavorite.setImageResource(R.drawable.infavorite);
        else
            btnFavorite.setImageResource(R.drawable.nofavorite);
        return view;
    }
}

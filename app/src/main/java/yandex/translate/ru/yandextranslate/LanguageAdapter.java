package yandex.translate.ru.yandextranslate;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Илья on 06.04.2017.
 */

public class LanguageAdapter extends BaseAdapter {
    Context ctx;
    LayoutInflater lInflater;
    ArrayList<Language> languages;
    int selected;

    LanguageAdapter(Context context, ArrayList<Language> input_languages, int _selected) {
        ctx = context;
        languages = input_languages;
        lInflater = (LayoutInflater) ctx
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        selected = _selected;
    }

    // кол-во элементов
    @Override
    public int getCount() {
        return languages.size();
    }

    // элемент по позиции
    @Override
    public Object getItem(int position) {
        return languages.get(position);
    }

    // id по позиции
    @Override
    public long getItemId(int position) {
        return position;
    }

    // пункт списка
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // используем созданные, но не используемые view
        View view = convertView;
        if (view == null) {
            view = lInflater.inflate(R.layout.language_item, parent, false);
        }

        TextView tvFullName=(TextView) view.findViewById(R.id.tvFullName);
        LinearLayout layoutLanguage = (LinearLayout) view.findViewById(R.id.layoutLanguage);

        layoutLanguage.setBackgroundColor(Color.WHITE);

        Language l = (Language)getItem(position);
        if (l.id == selected) {
            layoutLanguage.setBackgroundColor(Color.rgb(225, 225, 225));
        }

        tvFullName.setTextColor(Color.BLACK); //Сбрасываем все настройки, которые только были для заголовка. Иногад они примерняются к другим
        tvFullName.setTextSize(20);
        tvFullName.setTypeface(null, Typeface.NORMAL);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT); //Задаем параметры лейаута

        llp.setMargins(40, 0, 0, 0);
        tvFullName.setLayoutParams(llp);

        if (l.id == -2) { //Задаем стиль для заголовков
            tvFullName.setTextColor(Color.rgb(80, 80, 80));
            tvFullName.setTextSize(14);
            layoutLanguage.setBackgroundColor(Color.rgb(180, 180, 180));
            tvFullName.setTypeface(null, Typeface.BOLD_ITALIC);

            llp.setMargins(90, 0, 0, 0);
            tvFullName.setLayoutParams(llp);
        }

        tvFullName.setText(l.fullName);

        view.setTag(l);

        return view;
    }

}

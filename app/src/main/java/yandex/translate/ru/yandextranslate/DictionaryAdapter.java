package yandex.translate.ru.yandextranslate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

import yandex.translate.ru.yandextranslate.DictionaryAnwser;
import yandex.translate.ru.yandextranslate.DictionaryTranslate;
import yandex.translate.ru.yandextranslate.Word;


public class DictionaryAdapter extends BaseAdapter {
    Context ctx;
    LayoutInflater lInflater;
    ArrayList<DictionaryAnwser> anwser;

    TextView tvNumber;
    TextView tvElements;
    TextView tvMeans;
    TextView tvExamples;

    DictionaryAdapter(Context context, ArrayList<DictionaryAnwser> _anwser) {
        ctx = context;
        anwser = _anwser;
        lInflater = (LayoutInflater) ctx
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // кол-во элементов
    @Override
    public int getCount() {
        return anwser.size();
    }

    // элемент по позиции
    @Override
    public Object getItem(int position) {
        return anwser.get(position);
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
            view = lInflater.inflate(R.layout.dictionary_item, parent, false);
        }

        tvNumber = (TextView) view.findViewById(R.id.tvNumber);
        tvElements = (TextView) view.findViewById(R.id.tvElements);
        tvMeans = (TextView) view.findViewById(R.id.tvMeans);
        tvExamples = (TextView) view.findViewById(R.id.tvExamples);

        tvMeans.setVisibility(View.VISIBLE); //Устанавливаем видимость элементов
        tvExamples.setVisibility(View.VISIBLE);

        if (anwser.get(position).means.size() == 0) //Убираем элементы, чтобы они не занимали место, если они пустые
            tvMeans.setVisibility(View.GONE);

        if (anwser.get(position).examples.size() == 0)
            tvExamples.setVisibility(View.GONE);

        tvNumber.setText(Integer.toString(position+1));

        String elements="";
        int i;
        for (i=0; i<anwser.get(position).elements.size(); i++) { //Выводим синонимы
            elements += anwser.get(position).elements.get(i);
            if (i<anwser.get(position).elements.size()-1)
                elements +=  ", ";
        }
        tvElements.setText(elements);

        String means="";
        for (i=0; i<anwser.get(position).means.size(); i++) { //Выводим значения
            means += anwser.get(position).means.get(i);
            if (i<anwser.get(position).means.size()-1)
                means +=  ", ";
        }
        if (means.length() > 0)
            means="(" + means + ")";

        tvMeans.setText(means);

        String examples="";
        for (i=0; i<anwser.get(position).examples.size(); i++) { //Выводим примеры
            examples += anwser.get(position).examples.get(i) + " " + "\u2014" + " " + anwser.get(position).examplesTrans.get(i);
            if (i<anwser.get(position).examples.size()-1) //Если это не последний пример -- переводим после него строку
                examples +=  "\n";
        }
        tvExamples.setText(examples);

        return view;
    }
}

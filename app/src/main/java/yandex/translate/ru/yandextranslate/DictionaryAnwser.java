package yandex.translate.ru.yandextranslate;

import java.util.ArrayList;

public class DictionaryAnwser {
    ArrayList<String> elements = new ArrayList<>();
    ArrayList<String> elementsPos = new ArrayList<>();
    ArrayList<String> means = new ArrayList<>();
    ArrayList<String> examples = new ArrayList<>();
    ArrayList<String> examplesTrans = new ArrayList<>();

    DictionaryAnwser(String anwser) {
        int start;
        int last;
        int finish;

        start = anwser.indexOf("{text=") + 6;
        last = anwser.indexOf(',', start);
        elements.add(anwser.substring(start, last));

        start = anwser.indexOf("pos=", last) + 4;

        if ((anwser.indexOf(',', start) != -1) && (anwser.indexOf(',', start) < anwser.indexOf('}', start))) //Присваиваем last позицию того, что раньше, } или ,
            last = anwser.indexOf(',', start);
        else
            last = anwser.indexOf('}', start);

        elementsPos.add(anwser.substring(start, last));

        start = anwser.indexOf("syn=", last)+4;

        if (start != 3) { //Заносим данные из синонимов. Проверяем, что поле syn есть
            finish = MainActivity.bracketsCounter(anwser.substring(start), true) + start;

            while ((anwser.indexOf("{text=", start) < finish) && (anwser.indexOf("{text=", start) != -1)) {
                start = anwser.indexOf("{text=", last) + 6;
                last = anwser.indexOf(',', start);
                elements.add(anwser.substring(start, last));

                start = anwser.indexOf("pos=", last) + 4;
                last = anwser.indexOf('}', start);
                elementsPos.add(anwser.substring(start, last));
            }
        }

        start = anwser.indexOf("mean=", last) + 5;

        if (start != 4) { //Заносим данные из переводов. Проверяем, что поле mean есть.
            finish = MainActivity.bracketsCounter(anwser.substring(start), true) + start;

            while ((anwser.indexOf("{text=", start) < finish) && (anwser.indexOf("{text=", start) != -1)) {
                start = anwser.indexOf("{text=", last) + 6;
                last = anwser.indexOf('}', start);
                means.add(anwser.substring(start, last));
            }
        }

        start = anwser.indexOf("ex=", last)+3;

        if (start != 2) { //Заносим данные из примеров
            finish = MainActivity.bracketsCounter(anwser.substring(start), true) + start;

            while ((anwser.indexOf("{text=", start) < finish) && (anwser.indexOf("{text=", start) != -1)) {
                start = anwser.indexOf("{text=", start) + 6;
                last = anwser.indexOf(',', start);
                examples.add(anwser.substring(start, last));

                start = anwser.indexOf("tr=[{", last) + 4;
                start = anwser.indexOf("{text=", start) + 6;
                last = anwser.indexOf('}', start);
                examplesTrans.add(anwser.substring(start, last));
            }
        }
    }
}

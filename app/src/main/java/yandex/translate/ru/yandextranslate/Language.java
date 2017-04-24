package yandex.translate.ru.yandextranslate;


public class Language implements Comparable<Language> {
    public int id;
    public String fullName;
    public String shortName;

    public Language(int _id, String _fullName, String _shortName) {
        id=_id;
        fullName=_fullName;
        shortName=_shortName;
    }

    public Language() {
        id=-1;
        fullName="";
        shortName="";
    }

    @Override
    public int compareTo(Language l) { //Сравниваем языки
        return (this.fullName.compareTo(l.fullName));
    }
}

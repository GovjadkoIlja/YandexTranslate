package yandex.translate.ru.yandextranslate;

public class Word {
    int id;
    String word;
    String translation;
    int lanFromId;
    String langFrom;
    int lanToId;
    String langTo;
    boolean favorite;

    public Word(int _id, String _word, String _translation, int _lanFromId, String _langFrom, int _lanToId, String _langTo, boolean _favorite) {
        id=_id;
        word=_word;
        translation=_translation;
        lanFromId=_lanFromId;
        langFrom=_langFrom;
        lanToId=_lanToId;
        langTo=_langTo;
        favorite=_favorite;
    }
}

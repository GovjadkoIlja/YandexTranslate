package yandex.translate.ru.yandextranslate;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Created by Илья on 19.04.2017.
 */

public interface DictionaryTranslate { //Интерфейс для получения перевода от словаря
    @FormUrlEncoded
    @POST("/api/v1/dicservice.json/lookup")
    Call<Object> translate(@FieldMap Map<String, String> map);
}

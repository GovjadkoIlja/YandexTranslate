package yandex.translate.ru.yandextranslate;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface DictionaryRecieve { //Интерфейс для получения списка языков словаря
    @FormUrlEncoded
    @POST("/api/v1/dicservice.json/getLangs")
    Call<Object> receive(@FieldMap Map<String, String> map);
}

package Strategy;

public class Incremental_Strategy implements Url_Shortner_Strategy{
    private static Integer ournumber= 999;

    private String ShortnerUrl(String url){
        ournumber++;
        return INTIALPART+String.valueOf(ournumber);
    }
    @Override
    public String short_url(String OrignalUrl) {
        return ShortnerUrl(OrignalUrl);
    }
}
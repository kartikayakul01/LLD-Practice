package Strategy;
public interface Url_Shortner_Strategy {
    String INTIALPART = "https://ourwebsite/";

    String short_url(String OrignalUrl);
}
import models.URL_Shortner;

import java.util.Scanner;

class main{
    private static Scanner sc = new Scanner(System.in);
    private static URL_Shortner api = new URL_Shortner();
    private static void create_url_api(){
        System.out.println("Enter the url you want to short = ");
        String Orignal_url= sc.next();
        String short_url =api.createUrl(Orignal_url);
        System.out.println("Send people this = "+short_url);
    }
    private static void repeat_ask(){
        System.out.println("TELL US : \n"
                + "press 1 : Create a new URL \n"+
                "press 2 : get url  \n"+
                "press 3 : Exit \n"
                + "Enter Please = "
        );
    }
    private static void get_url_api(){

        System.out.println("Give that url = ");
        String short_url= sc.next();
        String Orignal_url =api.getUrl(short_url);
        System.out.println("redirected to this  = "+Orignal_url);
    }
    public static void main(String[] args){

        System.out.printf("Welcome to URL SHORTERNER \n");
        Boolean running= true;

        while(running){

            repeat_ask();
            URL_Enum selected_option = null;
            int Pressed_button= sc.nextInt();


            for(URL_Enum option: URL_Enum.values()){
                if(option.getValue()==Pressed_button){
                    selected_option = option;
                    break;
                }
            }
            if(selected_option==null){
                System.out.println("Not a valid option chosen \n \n");
                continue;
            }

            switch (selected_option){
                case URL_Enum.EXIT -> {
                    System.out.println("EXITING");
                    running=false;
                }
                case URL_Enum.GetURL -> {
                    get_url_api();
                    break;
                }
                case URL_Enum.SendURL -> {
                    create_url_api();
                    break;
                }
            }
        }
    }

}
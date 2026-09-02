import models.URL_Shortner;

import java.util.Scanner;

class main{
    private static void repeat_ask(){
        System.out.println("TELL US : \n"
                + "press 1 : Create a new URL \n"+
                "press 2 : get url  \n"+
                "press 3 : Exit \n"
                + "Enter Please = "
        );
    }
    public static void main(String[] args){
        URL_Shortner api = new URL_Shortner();
        System.out.printf("Welcome to URL SHORTERNER \n");
        Boolean running= true;
        Scanner sc = new Scanner(System.in);

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
                    break;
                }
                case URL_Enum.SendURL -> {
                    continue;
                }
            }
        }
        String x="https://water/";

        System.out.printf("\n"+api.createUrl(x));
    }

}
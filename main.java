import Controller.Url_Shortner_Controller;
import Repository.Url_Shortner_Repository;
import Strategy.Incremental_Strategy;
import Strategy.Url_Shortner_Strategy;
import models.URL_Shortner;
import service.Url_Shortner_Service;

import java.util.Scanner;

public class main { // Capital 'M' standard follow karne ke liye
    private static Scanner sc = new Scanner(System.in);
    // In saare variables ko static kiya taaki static main method inhe use kar sake
    private static Url_Shortner_Repository storage;
    private static Url_Shortner_Strategy incrementStrategy;
    private static Url_Shortner_Service service;
    private static Url_Shortner_Controller controller;

    private static void create_url_api(){
        System.out.print("Enter the url you want to short = ");
        String Orignal_url = sc.next();
        String short_url = controller.create_url(Orignal_url);
        System.out.println("Send people this = " + short_url + "\n");
    }

    private static void repeat_ask(){
        System.out.println("TELL US : \n"
                + "press 1 : Create a new URL \n"
                + "press 2 : get url  \n"
                + "press 3 : Exit"
        );
        System.out.print("Enter Please = ");
    }

    private static void get_url_api() throws Exception {
        System.out.print("Give that url = ");
        String short_url = sc.next();
        String Orignal_url = controller.read_url(short_url);
        System.out.println("redirected to this  = " + Orignal_url + "\n");
    }

    // FIXED: static keyword add kiya
    public static void main(String[] args){
        System.out.println("-----------------Initializing------------ \n");
        System.out.println("-----------------Starting DB , Starting Services------------ \n");

        storage = new Url_Shortner_Repository();
        incrementStrategy = new Incremental_Strategy();
        service = new Url_Shortner_Service(storage, incrementStrategy);
        controller = new Url_Shortner_Controller(service);

        System.out.println("----------------- DB Started ,Services Started------------ \n");
        System.out.println("Welcome to URL SHORTENER \n");

        boolean running = true; // Primitive boolean lower-case use karein

        while(running){
            repeat_ask();
            URL_Enum selected_option = null;

            // Scanner input safety (agar user text daal de galti se)
            if(!sc.hasNextInt()) {
                System.out.println("Please enter a valid number!\n");
                sc.next(); // invalid input clear karne ke liye
                continue;
            }

            int Pressed_button = sc.nextInt();

            for(URL_Enum option : URL_Enum.values()){
                if(option.getValue() == Pressed_button){
                    selected_option = option;
                    break;
                }
            }

            if(selected_option == null){
                System.out.println("Not a valid option chosen \n");
                continue;
            }

            switch (selected_option){
                case EXIT -> {
                    System.out.println("EXITING...");
                    running = false;
                }
                case GetURL -> {
                    try {
                        get_url_api();
                    } catch (Exception e) {
                        // Aapki requirement ke mutabik: Sirf error message print hoga stack trace nahi
                        System.out.println("Error: " + e.getMessage() + "\n");
                    }
                }
                case SendURL -> {
                    try {
                        create_url_api();
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage() + "\n");
                    }
                }
            }
        }

        sc.close();
    }
}

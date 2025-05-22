package com.etiya;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Soru-Cevap Uygulaması");
        System.out.print("Sorunu Sor: ");
        String question = scanner.nextLine();

        QAAssistant assistant = new QAAssistant();
        assistant.startConversation(question);

        System.out.println("\nCevabımı begendin mi? (evet/hayir): ");
        String feedback = scanner.nextLine();

        assistant.provideFeedback(feedback);

        scanner.close();
    }
}
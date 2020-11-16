package com.chengyi.chendb;

import java.util.Scanner;

public class Chendb {
    public static void main(String[] args) {
        Chendb chendb = new Chendb();
        chendb.start();
    }

    private void start() {
        System.out.println("welcome to use chendb");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String command = scanner.next();
            if (command.equals(Constants.exitCommand)) {
                System.out.println("bye~");
                break;
            }
            command = standardify(command);
            if (!isValid(command)) {
                System.out.println("invalid command, input again");
            }

        }
    }

    private String standardify(String command) {
        return command.trim().replaceAll(" +", " ").toLowerCase();
    }

    private boolean isValid(String command) {
        return true;
    }
}

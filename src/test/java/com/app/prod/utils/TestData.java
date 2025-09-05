package com.app.prod.utils;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class TestData {

    private static Random random = new Random();

    public static List<String> firstNames = List.of(
            "John",
            "Emily",
            "Michael",
            "Sarah",
            "David",
            "Laura",
            "Daniel",
            "Emma",
            "James",
            "Olivia",
            "Robert",
            "Sophia",
            "William",
            "Isabella",
            "Matthew",
            "Charlotte",
            "Joseph",
            "Amelia",
            "Christopher",
            "Mia"
    );

    public static List<String> lastNames = List.of(
            "Smith",
            "Johnson",
            "Brown",
            "Davis",
            "Wilson",
            "Miller",
            "Anderson",
            "Thomas",
            "Taylor",
            "Moore",
            "Jackson",
            "White",
            "Harris",
            "Martin",
            "Thompson",
            "Garcia",
            "Martinez",
            "Robinson",
            "Clark",
            "Lewis"
    );

    public static String firstName(){
        return firstNames.get(random.nextInt(0, firstNames.size() - 1));
    }

    public static String lastName(){
        return firstNames.get(random.nextInt(0, firstNames.size() - 1));
    }

    public static String password(){
        return UUID.randomUUID().toString().substring(0, 8);
    }

}

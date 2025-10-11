package com.app.prod.utils;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class TestData {

    private static Random random = new Random();

    public static final List<String> firstNames = List.of(
            "John", "Emily", "Michael", "Sarah", "David", "Laura",
            "Daniel", "Emma", "James", "Olivia", "Robert", "Sophia",
            "William", "Isabella", "Matthew", "Charlotte", "Joseph",
            "Amelia", "Christopher", "Mia"
    );

    public static final List<String> lastNames = List.of(
            "Smith", "Johnson", "Brown", "Davis", "Wilson", "Miller",
            "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "White",
            "Harris", "Martin", "Thompson", "Garcia", "Martinez",
            "Robinson", "Clark", "Lewis"
    );

    private static final List<String> pollTitles = List.of(
            "Community Parking Rules",
            "New Garden Project",
            "Renovation of Hallway A",
            "Playground Upgrade Proposal",
            "Budget Allocation 2025",
            "New Elevator Installation",
            "Security Cameras Expansion",
            "Waste Management Improvements",
            "Gym Access for Residents",
            "Painting of Building Facade"
    );

    private static final List<String> pollDescriptions = List.of(
            "Residents are asked to vote on whether parking spaces should be reassigned.",
            "A proposal for adding new plants and benches to the common garden.",
            "Vote on whether to repaint and replace lighting in Hallway A.",
            "Choose preferred options for upgrading the playground area.",
            "Decide how the annual maintenance budget should be distributed.",
            "Vote on whether to allocate funds for an additional elevator.",
            "Proposal to expand camera coverage in the building entrances.",
            "Vote on implementing a new waste sorting and recycling system.",
            "Proposal for granting 24/7 access to the building gym for all residents.",
            "Vote on the new color scheme and materials for the facade renovation."
    );

    public static String title() { return randomElement(pollTitles);}

    public static String description() { return randomElement(pollDescriptions);}

    public static String firstName(){
        return randomElement(firstNames);
    }

    public static String lastName(){
        return randomElement(lastNames);
    }

    public static String password(){
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static <T> T randomElement(List<T> data){
        return data.get(random.nextInt(0, data.size() - 1));
    }

}

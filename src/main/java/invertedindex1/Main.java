package invertedindex1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

class Main {
    static HashMap<String, HashSet<String>> store = new HashMap<>();

    static ArrayList<String> tokeniseAndNormalise(String str) {
        ArrayList<String> ans = new ArrayList<>();
        Scanner scanner = new Scanner(str);
        while(scanner.hasNext()) {
            ans.add(scanner.next().toLowerCase());
        }
        return ans;
    }

    static void seedDoc(Path path) {
        try {
            // read the file
            String content = Files.readString(path);
            // tokenize it
            // normalise it by converting to lowercase
            ArrayList<String> tokenizedAndNormalised = tokeniseAndNormalise(content);
            // put it into store
            for(int i=0; i<tokenizedAndNormalised.size(); i++) {
                String key = tokenizedAndNormalised.get(i);
                if(store.containsKey(key)) {
                    HashSet<String> postingList = store.get(key);
                    postingList.add(path.toString());
                } else {
                    HashSet<String> newPostingList = new HashSet<>();
                    newPostingList.add(path.toString());
                    store.put(key, newPostingList);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    static ArrayList<String> Search(String givenText) {
        ArrayList<String> givenTextNormalised = tokeniseAndNormalise(givenText);
        if (givenTextNormalised.isEmpty()) {
            return new ArrayList<String>();
        }
        String firstWord = givenTextNormalised.get(0);
        if (!store.containsKey(firstWord)) {
            return new ArrayList<String>();
        }
        HashSet<String> resultSet = new HashSet<>(store.get(firstWord));
        for(int i=1; i<givenTextNormalised.size(); i++) {
            String key = givenTextNormalised.get(i);
            if (store.containsKey(key)) {
                resultSet.retainAll(store.get(key));
            } else {
                return new ArrayList<String>(); 
            }
        }
        ArrayList<String> fileContents = new ArrayList<>();

        for (String path : resultSet) {
            try {
                String content = Files.readString(Paths.get(path));
                fileContents.add(content);
            } catch (IOException e) {
                System.err.println("Error reading file during appending: " + e.getMessage());
            }
        }
        return fileContents;
    }

    public static void main(String[] args) {
        // seed all docs
        for(int i=1; i<=10; i++) {
            String resourcePath = String.format("Data/file%d.txt", i);
            try {
                // Get the file's URL from the classpath
                java.net.URL fileUrl = Main.class.getClassLoader().getResource(resourcePath);

                if (fileUrl == null) {
                    System.err.println("Error: Cannot find file on classpath: " + resourcePath);
                    continue; // Skip to the next file
                }

                // Convert the URL to a Path and pass it to your method
                seedDoc(Paths.get(fileUrl.toURI()));

            } catch (Exception e) {
                System.err.println("Error reading file: " + resourcePath);
                e.printStackTrace();
            }
        }
        System.out.println("Inverted Index 1");
        ArrayList<String> results1 = Search("quick brown");
        System.out.println("Lookup for `quick brown`:" + results1);
        ArrayList<String> results2 = Search("a fox");
        System.out.println("Lookup for `a fox`:" + results2);
        ArrayList<String> results4 = Search("run");
        System.out.println("Lookup for `run`:" + results4);
        ArrayList<String> results5 = Search("good programmer");
        System.out.println("Lookup for `good programmer`:" + results5);
    }
    
}
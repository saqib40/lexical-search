package invertedindex5;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

class Main {
    static HashMap<String, HashMap<String, Integer>> store = new HashMap<>();
    static HashMap<String, Integer> docLengths = new HashMap<>();
    static int totalDocs = 0;

    static final HashSet<String> stopWords = new HashSet<>(
        Arrays.asList(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", 
            "from", "has", "he", "in", "is", "it", "its", "of", 
            "on", "that", "the", "to", "was", "were", "will", "with"
        )
    );

    static Properties props;
    static StanfordCoreNLP pipeline;

    static {
        props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        pipeline = new StanfordCoreNLP(props);
    }

    static ArrayList<String> tokeniseAndNormalise(String str) {
        ArrayList<String> ans = new ArrayList<>();
        
        CoreDocument document = pipeline.processToCoreDocument(str);
        
        for (CoreLabel token : document.tokens()) {
            String lemma = token.lemma().toLowerCase();
            if (!stopWords.contains(lemma)) {
                ans.add(lemma);
            }
        }
        return ans;
    }

    static void seedDoc(Path path) {
        try {
            String content = Files.readString(path);
            ArrayList<String> tokenizedAndNormalised = tokeniseAndNormalise(content);
            docLengths.put(path.toString(), tokenizedAndNormalised.size());
            HashMap<String, Integer> termCounts = new HashMap<>();
            for(String term : tokenizedAndNormalised) {
                if(termCounts.containsKey(term)) {
                    termCounts.put(term, termCounts.get(term)+1);
                } else {
                    termCounts.put(term, 1);
                }
            }
            for (String term : termCounts.keySet()) {
                int frequency = termCounts.get(term);
                if (!store.containsKey(term)) {
                    store.put(term, new HashMap<>());
                }
                HashMap<String, Integer> postingsList = store.get(term);
                postingsList.put(path.toString(), frequency);
            }
            totalDocs++;
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    static class DocScore implements Comparable<DocScore> {
        String docContent;
        double score;
        public DocScore(String docContent, double score) {
            this.docContent = docContent;
            this.score = score;
        }
        // To sort in descending order, because by default it's ascending
        public int compareTo(DocScore other) {
            return Double.compare(other.score, this.score);
        }
        @Override
        public String toString() {
            // Let's make a nice preview of the content
            String preview = docContent.replace("\n", " ");
            if (preview.length() > 50) {
                preview = preview.substring(0, 50) + "...";
            }
            // Return a formatted string instead of the memory address
            return String.format("\n\t[Score: %.4f] -> \"%s\"", this.score, preview);
        }
    }

    static ArrayList<DocScore> Search(String givenText) {
        ArrayList<String> queryTerms = tokeniseAndNormalise(givenText);
        if (queryTerms.isEmpty()) {
            return new ArrayList<DocScore>();
        }
        // essence isn't intersection here
        // essence is the score
        HashMap<String, Double> docScores = new HashMap<>();
        for(String term : queryTerms) {
            if (!store.containsKey(term)) {
                continue;
            }
            HashMap<String, Integer> postingsList = store.get(term);
            int docFreq = postingsList.size();
            double idf = Math.log((double)totalDocs / docFreq);
            for (String docId : postingsList.keySet()) {
                int tf_raw = postingsList.get(docId);
                int docLength = docLengths.get(docId);
                double tf = (double)tf_raw / docLength;
                
                double tfIdfScore = tf * idf;
                
                double currentScore = docScores.getOrDefault(docId, 0.0);
                docScores.put(docId, currentScore + tfIdfScore);
            }
        }
        ArrayList<DocScore> sortedScores = new ArrayList<>();
        for (String docId : docScores.keySet()) {
            try {
                String content = Files.readString(Paths.get(docId));
                sortedScores.add(new DocScore(content, docScores.get(docId)));
            } catch (IOException e) {
                System.err.println("Error reading file during appending: " + e.getMessage());
            }
        }
        Collections.sort(sortedScores);
        return sortedScores;
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
        ArrayList<DocScore> results1 = Search("quick brown");
        System.out.println("Lookup for `quick brown`:" + results1);
        ArrayList<DocScore> results2 = Search("a fox");
        System.out.println("Lookup for `a fox`:" + results2);
        ArrayList<DocScore> results4 = Search("run");
        System.out.println("Lookup for `run`:" + results4);
        ArrayList<DocScore> results5 = Search("good programmer");
        System.out.println("Lookup for `good programmer`:" + results5);
    }
    
}
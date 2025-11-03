// I've named this invertedindex6 to show the evolution
package invertedindex6;

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
    static double avgDocLength = 0;
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
                termCounts.put(term, termCounts.getOrDefault(term, 0) + 1);
            }
            for (String term : termCounts.keySet()) {
                if (!store.containsKey(term)) {
                    store.put(term, new HashMap<>());
                }
                store.get(term).put(path.toString(), termCounts.get(term));
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
        public int compareTo(DocScore other) {
            return Double.compare(other.score, this.score);
        }
        @Override
        public String toString() {
            String preview = docContent.replace("\n", " ");
            if (preview.length() > 50) {
                preview = preview.substring(0, 50) + "...";
            }
            return String.format("\n\t[Score: %.4f] -> \"%s\"", this.score, preview);
        }
    }

    // search method evolves to BM25
    static ArrayList<DocScore> Search(String givenText) {
        ArrayList<String> queryTerms = tokeniseAndNormalise(givenText);
        if (queryTerms.isEmpty()) {
            return new ArrayList<DocScore>();
        }
        
        // tuning knobs
        final double k1 = 1.2;
        final double b = 0.75;
        
        HashMap<String, Double> docScores = new HashMap<>();

        for(String term : queryTerms) {
            if (!store.containsKey(term)) {
                continue;
            }
            
            HashMap<String, Integer> postingsList = store.get(term);
            int docFreq = postingsList.size();
            
            double idf = Math.log(
                ((double)totalDocs - docFreq + 0.5) / (docFreq + 0.5) + 1.0
            );

            for (String docId : postingsList.keySet()) {
                int tf_raw = postingsList.get(docId);
                int docLength = docLengths.get(docId);
                
                double tf_bm25 = (tf_raw * (k1 + 1)) / 
                                 (tf_raw + k1 * (1 - b + b * (docLength / avgDocLength)));
                
                double bm25Score = idf * tf_bm25;
                
                double currentScore = docScores.getOrDefault(docId, 0.0);
                docScores.put(docId, currentScore + bm25Score);
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
        for(int i=1; i<=10; i++) {
            String resourcePath = String.format("Data/file%d.txt", i);
            try {
                java.net.URL fileUrl = Main.class.getClassLoader().getResource(resourcePath);
                if (fileUrl == null) {
                    System.err.println("Error: Cannot find file on classpath: " + resourcePath);
                    continue; 
                }
                seedDoc(Paths.get(fileUrl.toURI()));
            } catch (Exception e) {
                System.err.println("Error reading file: " + resourcePath);
                e.printStackTrace();
            }
        }

        double totalLength = 0;
        for (int length : docLengths.values()) {
            totalLength += length;
        }
        avgDocLength = totalLength / totalDocs;
        
        System.out.println("--- Indexing complete. avgDocLength: " + avgDocLength + " ---");

        
        System.out.println("Lookup for `quick brown`:");
        ArrayList<DocScore> results1 = Search("quick brown");
        System.out.println(results1);
        
        System.out.println("\nLookup for `learning search`:");
        ArrayList<DocScore> results2 = Search("learning search");
        System.out.println(results2);
        
        System.out.println("\nLookup for `run`:");
        ArrayList<DocScore> results4 = Search("run");
        System.out.println(results4);
    }
}

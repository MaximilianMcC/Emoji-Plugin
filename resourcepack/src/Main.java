package resourcepack.src;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Main {
    
    public static void main(String[] args) {

        System.out.println("Emoji resourcepack generator\n");

        // Ask for what emoji style they want
        System.out.println("Please choose an emoji style:");
        System.out.println("1)\tTwemoji/Discord");
        System.out.println("2)\tApple");
        
        Scanner scanner = new Scanner(System.in);
        int emojiStyleInput = scanner.nextInt();
        scanner.close();

        // Set the emoji style (default is twemoji)
        EmojiStyle emojiStyle = EmojiStyle.TWEMOJI;
        if (emojiStyleInput == 2) emojiStyle = EmojiStyle.APPLE;



        // Get the list of emojis, their shortcode, and the url to the image
        System.out.println("Getting emojis\nThis could take a while.");
        ArrayList<Emoji> emojis = new ArrayList<Emoji>();

        try {

            // Get the website, so we can scrape it for emojis
            Document emojiWebsite = Jsoup.connect("https://www.emojibase.com/").get();

            // Get all of the emojis from the different categories
            Elements emojiList = emojiWebsite.select("table tbody tr");
            System.out.println("Total emojis found: " + emojiList.size());

            // Iterate over each emoji. Go to the page, then get the image, and shortcode.
            long startTime = System.currentTimeMillis();
            long lastUpdateTime = startTime;

            for (int i = 25; i < emojiList.size(); i++) {

                // Get the link to the current emoji, then scrape it
                String currentEmojiLink = emojiList.get(i).selectFirst("td a").attr("href");
                Document currentEmojiPage = Jsoup.connect("https://www.emojibase.com" + currentEmojiLink).get();

                // Get the shortcode
                String shortcode = currentEmojiPage.selectFirst("table tbody tr td:nth-child(6)").text();
                if (shortcode.equals("::")) continue;

                // Get the hex code
                String hexCode = currentEmojiPage.selectFirst("table tbody tr td:nth-child(2)").text();

                // Get the image url
                Elements images = currentEmojiPage.select("div.panel img");
                String imageUrl = "";
                if (emojiStyle == EmojiStyle.TWEMOJI) imageUrl += images.get(3).attr("src"); // Twemoji
                else if (emojiStyle == EmojiStyle.APPLE) imageUrl += images.get(2).attr("src"); // Apple

                // Save the emoji in the emojis list
                Emoji emoji = new Emoji(shortcode, hexCode, imageUrl);
                emojis.add(emoji);



                // Get the average time remaining
                long currentTime = System.currentTimeMillis();
                lastUpdateTime = currentTime;
                long totalTime = currentTime - startTime;

                double averageDownloadTime = totalTime / (i + 1.0);
                double averageRemainingTime = averageDownloadTime * (emojiList.size() - i - 1);
                
                // Print it out
                System.out.print("\rGetting emoji with index of " + i + " - Average time remaning: " + formatTime(averageRemainingTime));

            }


        } catch (IOException e) {
            System.out.println("\nError whilst getting emojis:");
            e.printStackTrace();
        }


        // Make the resourcepack root directory
        String resourcePackFolder = "./Emojis";
        if (emojiStyle == EmojiStyle.TWEMOJI) resourcePackFolder += " (Twemoji)";
        else if (emojiStyle == EmojiStyle.APPLE) resourcePackFolder += " (Apple)";
        File resourcePackRoot = new File(resourcePackFolder);
        resourcePackRoot.mkdir();

        // Make the mcmeta file
        try {
            File mcmetaFile = new File(resourcePackRoot, "pack.mcmeta");
            mcmetaFile.createNewFile();
            FileWriter fileWriter = new FileWriter(mcmetaFile);
            fileWriter.write("{ \"pack\": { \"pack_format\": 9, \"description\": \"Emojis for Minecraft\" } }");
            fileWriter.close();

        } catch (Exception e) {
            System.err.println("\nError while making mcmeta file:");
            e.printStackTrace();
        }

        // Make the assets and minecraft directories
        File minecraftFolder = new File(resourcePackRoot, "/assets/minecraft");
        minecraftFolder.mkdirs();

        // Make the font folder, and json file inside
        File fontFolder = new File(minecraftFolder, "font");
        fontFolder.mkdir();
        try {
            File mcmetaFile = new File(fontFolder, "default.json");
            mcmetaFile.createNewFile();
            FileWriter fileWriter = new FileWriter(mcmetaFile);

            //TODO: Get a json library to make this easier
            // Add the start of the json file
            fileWriter.write("{ \"providers\": [");

            // Add all of the emojis
            for (int i = 0; i < emojis.size(); i++) {
                
                // Add the current emoji json
                String emojiJson = "{ \"type\": \"bitmap\", \"file\": \"minecraft:font/" + emojis.get(i).filename + ".png\", \"ascent\": 7, \"height\": 7, \"chars\": [ \"\\u" + emojis.get(i).hex + "\" ] }";
                if (i < (emojis.size() - 1)) emojiJson += ",";

                System.out.print("\rAdding emoji with index of " + i + " to resourcepack JSON");
                fileWriter.write(emojiJson);
            }

            // Add the end of the json file
            fileWriter.write("]}");
            fileWriter.close();

        } catch (Exception e) {
            System.err.println("\nError while making default fonts file:");
            e.printStackTrace();
        }


    }

    public static String formatTime(double timeInMs) {

        int seconds = (int)(timeInMs / 1000);
        int minutes = (int)(seconds / 60);
        int remainingSeconds = (int)(seconds % 60);
        
        if (minutes == 0) {
            return remainingSeconds + " seconds";
        } else {
            if (minutes > 1) return minutes + " minutes, " + remainingSeconds + " seconds";
            return minutes + " minute, " + remainingSeconds + " seconds";
        }
    }
    

}

package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Wahda {

	private static String username = "";

	// Helper functions

	@SuppressWarnings("unchecked")
	private static <T> ArrayList<T> stringToArrayList(String str) { // Convers string representation of ArrayList into ArrayList
		var list = new ArrayList<T>();

		for (String element : str.replace("[", "").replace("]", "").replace("[]", "").replace("[, ", "").split(", ")) list.add((T) element);
		
		return list;
	}

	private static String getMd5(String input) { // Runs MD5 one-way hash on given string
		try {
			MessageDigest msgDst = MessageDigest.getInstance("MD5");

			byte[] msgArr = msgDst.digest(input.getBytes());
			BigInteger bi = new BigInteger(1, msgArr);

			String hshtxt = bi.toString(16);
			while (hshtxt.length() < 32) hshtxt = "0" + hshtxt;
			
			return hshtxt;
		} catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); }
	}

	private static String randomString() { // Generates random string of visible characters of length 24
		String generatedString = "";
		
		for (int i = 0; i < 24; i++) generatedString += (char) (new Random().nextInt(126 + 1 - 33) + 33);
		return generatedString;
	}

	private static String read(String filePath) { // Reads file from file system and returns contents
		try { return Files.readString(Path.of(filePath)); } catch (IOException e) { e.printStackTrace(); }
		return "";
	}

	private static int clamp(int val, int min, int max) { // Clamps value from minimum to maximum value
		return (val < min) ? min : (val > max) ? max : val;
	}
	
	private static void login(Scanner sc, PrintWriter pr, BufferedReader bf) throws IOException {
			while (true) { // Input validation loop
				System.out.print("\nPlease enter your token: ");
				var token = sc.nextLine();

				pr.println("GETALL, 1"); // Get tokens table
				pr.flush();
				var tokens = bf.readLine(); // Tokens table

				if (!stringToArrayList(tokens).contains(getMd5(token))) System.out.println("\nToken not recognised."); // Is token's hash not on the	table?
				else { // Runs if token exists
					pr.println("GETALL, 0"); // Get names table
					pr.flush();
					var names = bf.readLine(); // Names table

					username = (String) stringToArrayList(names).get(stringToArrayList(tokens).indexOf(getMd5(token)));
					
					// Sets Username to username from database based on
					// index of token's hash

					System.out.println("\nWelcome, " + username + "!");
					
					break;
				}
			}
	}

	private static void register(Scanner sc, PrintWriter pr, BufferedReader bf) throws IOException {
		while (true) { // Input validation loop
			System.out.print("\nPlease enter a username: ");
			String name = sc.nextLine();

			pr.println("GETALL, 0"); // Gets names table
			pr.flush();
			var names = bf.readLine(); // Names table

			if (!names.contains(name)) { // Is name unique?
				pr.println("ADD, " + "0, " + name); // Adds username onto database
				pr.flush();

				while (true) { // Makes sure token generated is unique
					String randomString = randomString(); // Generates random string

					if (!names.contains(randomString)) { // Is token unique?
						System.out.println("\nThis token will be required to log in: " + randomString);

						pr.println("ADD, " + "1, " + getMd5(randomString)); // Adds token onto database
						pr.flush();
						break;
					}
				}
				break;
			} else System.out.println("\nSorry but this username is unavailable."); // Username is not unique
		}
	}
	
	private static void application(Scanner sc, PrintWriter pr, BufferedReader bf) throws IOException {
		while (true) {
			System.out.print("\n[" + username + "] ~ $> "); // "Command" prefix

			String[] splittedCommand = sc.nextLine().split(" ");

			if (splittedCommand[0].equals("-blogs")) { // If command is -blogs
				String[] minMax = splittedCommand[1].split("~"); // Splits second part of the command on '~' to provide a min and max value

				pr.println("GETALL, 3"); // Gets blog names table
				pr.flush();

				var blogs = bf.readLine(); // Blog names
				var blogsMap = stringToArrayList(blogs); // Blogs as a ArrayList

				if (Integer.parseInt(minMax[0]) < Integer.parseInt(minMax[1])) for (int i = clamp(Integer.parseInt(minMax[0]), 0, blogsMap.size()); i < clamp(Integer.parseInt(minMax[1]), 0, blogsMap.size()); i++) System.out.println("\n[" + i + "] >> " + blogsMap.get(i));
				else for (int i = clamp(Integer.parseInt(minMax[0]), 0, blogsMap.size()); i > clamp(Integer.parseInt(minMax[1]), 0, blogsMap.size()); i--) System.out.println("\n[" + i + "] >> " + blogsMap.get(i));

				// Prints out blogs with indexes from minimum to maximum value
			} else if (splittedCommand[0].equals("-all")) { // If command is -all
				pr.println("GETALL, 3"); // Gets blog names table
				pr.flush();

				var blogs = bf.readLine(); // Blog names
				var blogsMap = stringToArrayList(blogs); // Blog names as a ArrayList

				for (int i = blogsMap.size() - 1; i >= 0; i--) System.out.println("\n[" + i + "] >> " + blogsMap.get(i)); // Prints out all blogs' names
			} else if (splittedCommand[0].equals("-view")) { // If command is -view
				pr.println("GET, 2, " + splittedCommand[1]); // Gets blog
				pr.flush();

				var blog = bf.readLine(); // Blogs table

				System.out.println("\n" + blog.replace("|NLC|", System.lineSeparator()).replace("|COMMA|", ", ")); // Prints out blog
			} else if (splittedCommand[0].equals("-create")) { // If command is -create
				String title = "";

				for (int i = 1; i < splittedCommand.length; i++) title += (i == splittedCommand.length - 1) ? splittedCommand[i] : splittedCommand[i] + " ";
				// Adds all arguments after the command token to "title" and adds a space after each except the last

				pr.println("ADD, 3, " + title + " : " + username + " @ " + DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now())); // Adds name to names table
				pr.flush();

				System.out.print("\nPlease enter the path to your blog: ");
				String path = sc.nextLine(); // Path to blog

				pr.println("ADD, 2, " + ("|NLC|" + title + " : " + username + " @ " + DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()) + "|NLC||NLC|" + read(path).replace(", ", "|COMMA|").replace(System.lineSeparator(), "|NLC|").replace("\n", "|NLC|").replace("\r", "|NLC|"))); // Adds blog to blog table
				pr.flush();

			} else if (splittedCommand[0].equals("-exit")) { // If command is -exit
				System.out.println("\nThank you for using Wahda! Press enter to exit...");
				sc.nextLine();
				break;
			}
		}
	}
	
	public static void main(String[] args) {
		try (var s = new Socket("SERVER IP", 4999)) {

			// Resources

			var pr = new PrintWriter(s.getOutputStream()); 					// Output
			var in = new InputStreamReader(s.getInputStream()); 			// Input
			var bf = new BufferedReader(in);                    			// BufferedReader for input			
			
			// Main

			try (var sc = new Scanner(System.in)) {
				while (true) { // Login & Register loop
					System.out.print("\nLog in or register? (L / R) : ");
					String input = sc.nextLine();

					if (input.toLowerCase().equals("l")) { login(sc, pr, bf); break; }  // If input (to lowercase) is 'l' 
					else if (input.toLowerCase().equals("r")) { register(sc, pr, bf); } // If input is 'r' 
				 	else System.out.println("\nUnknown operation.");                    // Input was not 'l' or 'r'
				}
				
				application(sc, pr, bf);
			} catch (NumberFormatException | IOException e) { e.printStackTrace(); }
		} catch (UnknownHostException e) { e.printStackTrace(); } catch (IOException e) { e.printStackTrace(); }
	}
}

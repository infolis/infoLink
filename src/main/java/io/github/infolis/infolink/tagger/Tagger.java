package io.github.infolis.infolink.tagger;

import io.github.infolis.model.Chunk;
import io.github.infolis.model.TaggedWord;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Part-of-speech tagger using the TreeTagger
 * (http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/) command line
 * interface.
 *
 * @author katarina.boland@gesis.org
 *
 */
public class Tagger
{
	String tagCommand;
	String chunkCommand;
	String encoding;

	/**
	 * Class constructor specifying the command for invoking the tagger via
	 * command line interface, the command for invoking the phrase chunker via
	 * command line interface and the character encoding.
	 *
	 * @param tagCommand
	 *            the command for invoking the tagger via command line interface
	 * @param chunkCommand
	 *            the command for invoking the phrase chunker via command line
	 *            interface
	 * @param encoding
	 *            the character encoding
	 */
	public Tagger(String tagCommand, String chunkCommand, String encoding)
	{
		this.tagCommand = tagCommand;
		this.chunkCommand = chunkCommand;
		this.encoding = encoding;
	}

	/**
	 * Generates a map of Chunks from a TreeTagger output string. Keys: phrase
	 * tags. Values: phrase chunks
	 *
	 * @param file
	 *            TreeTagger output string containing information on phrase
	 *            chunks
	 * @return a map of Chunks representing the information in the TreeTagger
	 *         output string. Keys: tags, values: a list of Chunks
	 * @throws IOException
	 */
	public HashMap<String, ArrayList<Chunk>> getPhrases(String content) throws IOException
	{
		HashMap<String, ArrayList<Chunk>> phrases = new HashMap<String, ArrayList<Chunk>>();
		String[] wordInfoList = content.split(System.getProperty("line.separator"));
		String curTag = "";
		ArrayList<TaggedWord> curWords = new ArrayList<TaggedWord>();
		ArrayList<Chunk> curChunks;
		for (String wordInfo : wordInfoList)
		{
			// tree chunker output format: phraseStartTag \n input word \t tag
			// \t lemma \n phraseEndTag
			try
			{
				// found phraseEndTag - chunk completed
				if (wordInfo.startsWith("</") && wordInfo.endsWith(">"))
				{
					if (phrases.containsKey(curTag)) {
						curChunks = phrases.get(curTag);
					}
					else {
						curChunks = new ArrayList<Chunk>();
					}
					curChunks.add(new Chunk(curTag, curTag.replace("<", "</"), curWords));
					phrases.put(curTag, curChunks);
					curTag = "";
					curWords = new ArrayList<TaggedWord>();
				}
				else if (wordInfo.startsWith("<") && wordInfo.endsWith(">")) {
					curTag = wordInfo;
				}
				else
				{
					if (curTag.equals("")) {
						continue;
					}
					String[] infoParts = wordInfo.split("\t");
					// ignore lemmata - not needed here
					TaggedWord taggedWord = new TaggedWord(infoParts[0], infoParts[1]);
					curWords.add(taggedWord);
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		return phrases;
	}

	/**
	 * Applies the TreeTagger to tag a sentence (POS-tags).
	 *
	 * The TreeTagger is called via its command line interface using the
	 * commands provided at initiation of this Tagger instance. Temporary files
	 * are used to store the input and output of the tagger. To generate the
	 * list of TaggedWords from the temporary output file, the getTaggedSentence
	 * method is called.
	 *
	 * @param sentence
	 *            the sentence to be tagged
	 * @return the tagged sentence as a list of TaggedWords
	 * @throws IOException
	 */
	public ArrayList<TaggedWord> tag(String sentence) throws IOException
	{
		// tagger needs one word per line and punctuation as separate words
		// Util.writeToFile(new File(this.tempFileIn), this.encoding,
		// sentence.replaceAll("(\\p{Punct})", "$1 ").replaceAll("\\s+",
		// System.getProperty("line.separator")), false);
		System.out.println("tagging sentence \"" + sentence + "\"");
		Process p = Runtime.getRuntime().exec(
				new String[] { "/bin/sh", "-c", "echo \"" + sentence + "\" | " + this.tagCommand });
		InputStream in = new BufferedInputStream(p.getInputStream());
		InputStreamReader isr = new InputStreamReader(in);
		BufferedReader buff = new BufferedReader(isr);
		String output = "";
		String line;
		while ((line = buff.readLine()) != null) {
			output += System.getProperty("line.separator") + line;
		}
		in.close();
		buff.close();
		return getTaggedSentence(output.trim());
	}

	/**
	 * Extracts a list of TaggedWords from an output string created by the
	 * TreeTagger
	 *
	 * @param content
	 *            the TreeTagger output string containing words and their
	 *            POS-tags
	 * @return a list of TaggedWords representing the information in the
	 *         TreeTagger output string
	 * @throws IOException
	 */
	public ArrayList<TaggedWord> getTaggedSentence(String content) throws IOException
	{
		ArrayList<TaggedWord> taggedSentence = new ArrayList<TaggedWord>();
		String[] wordInfoList = content.split(System.getProperty("line.separator"));
		for (String wordInfo : wordInfoList)
		{
			// tree tagger output format: input word \t tag \t lemma
			try
			{
				String[] infoParts = wordInfo.split("\t");
				// ignore lemmata - not needed here
				TaggedWord taggedWord = new TaggedWord(infoParts[0], infoParts[1]);
				taggedSentence.add(taggedWord);
			} catch (ArrayIndexOutOfBoundsException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		return taggedSentence;
	}

	/**
	 * Applies the TreeTagger to chunk a string into phrases.
	 *
	 * The TreeTagger is called via its command line interface using the
	 * commands provided at initiation of this Tagger instance. Temporary files
	 * are used to store the input and output of the tagger. To generate the map
	 * of phrase chunks from the temporary output file, the getPhrases method is
	 * called.
	 *
	 * @param string
	 *            the string to be chunked into phrases
	 * @return a map of chunks having the phrase tags as keys and a list of
	 *         Chunks as values
	 * @throws IOException
	 */
	public HashMap<String, ArrayList<Chunk>> chunk(String string) throws IOException
	{
		// tagger needs one word per line and punctuation as separate words
		// Util.writeToFile(new File(this.tempFileIn),"utf-8",
		// string.replaceAll("(\\p{Punct})", "$1 ").replaceAll("\\s+",
		// System.getProperty("line.separator")), false);
		System.out.println("tagging \"" + string + "\"");

		Process p = Runtime.getRuntime().exec(
				new String[] { "/bin/sh", "-c", "echo \"" + string + "\" | " + this.chunkCommand });
		InputStream in = new BufferedInputStream(p.getInputStream());
		InputStreamReader isr = new InputStreamReader(in);
		BufferedReader buff = new BufferedReader(isr);
		String output = "";
		String line;
		while ((line = buff.readLine()) != null) {
			output += System.getProperty("line.separator") + line;
		}
		in.close();
		buff.close();
		return getPhrases(output.trim());
	}

	/**
	 * Prints chunks and POS tags of input text
	 *
	 * @param args
	 *            args[0]: tagCommand; args[1]: chunkCommand; args[2]: encoding;
	 *            args[3]: tempFileIn; args[4]: tempFileOut; args[5]: inputText
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException
	{
		if (args.length < 4) {
			System.out.println("Usage: Tagger <tagCommand> <chunkCommand> <encoding> <input>");
			System.out.println("	<tagCommand>	(example 'tree-tagger-german')");
			System.out.println("	<chunkCommand>	(example 'tagger-chunker-german')");
			System.out.println("	<encoding>	(example 'utf-8')");
			System.out.println("	<input>	input");
			System.exit(1);
		}
		String tagCommand = args[0];
		String chunkCommand = args[1];
		String encoding = args[2];
		String input = args[3];
		Tagger tagger = new Tagger(tagCommand, chunkCommand, encoding);
		System.out.println(tagger.chunk(input));
		System.out.println(tagger.tag(input));
	}
}

package com.thelincolnshome.CommandTool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;

/**
 *
 */
public final class Console
{
	private LineParser	lineParser;

	public Console(LineParser aPerser)
	{
		if(aPerser == null)
		{
			throw new IllegalArgumentException("Cannot be null.");
		}

		lineParser = aPerser;
	}

	public void run()
	{
		display(lineParser.getHelloPrompt());

		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		List<Object> result = new ArrayList<Object>();
		boolean notDone = false;
		String line = null;

		try
		{
			while(!notDone)
			{
				display("\n> ");

				line = stdin.readLine();

				//note that "result" is passed as an "out" parameter

				try
				{
					notDone = lineParser.parseInput(line, result);

					display(result);
				}
				catch(ParseException e)
				{
					display("I don't understand you!");
				}

				result.clear();
			}
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
		finally
		{
			IOUtils.closeQuietly(stdin);
		}
	}

	/**
	* Display some text to stdout.
	* The result of toString() is used.
	*/
	public static void display(Object aText)
	{
		if(aText != null)
		{
			System.out.print(aText.toString());
			System.out.flush();
		}
	}

	/**
	* Display a List of objects as text in stdout, in the order returned
	* by the iterator of aText.
	*/
	public static void display(Collection<Object> aText)
	{
		for(Object item : aText)
		{
			display(item);
			display("\n");
		}
	}

}

package com.thelincolnshome.CommandTool.DNS;

import java.util.HashMap;
import java.util.Set;

import com.thelincolnshome.CommandTool.LineParser;

public final class LineParsers
{
	private static final HashMap<String, LineParser>	parsers	= new HashMap<String, LineParser>();

	public static void register(LineParser inParser)
	{
		parsers.put(inParser.getName(), inParser);
	}

	public static LineParser getParser(String inName)
	{
		return parsers.get(inName);
	}

	public static Set<String> getParsers()
	{
		return parsers.keySet();
	}
}

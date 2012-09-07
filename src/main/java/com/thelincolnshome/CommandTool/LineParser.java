package com.thelincolnshome.CommandTool;

import java.util.List;

import org.apache.commons.cli.ParseException;

public interface LineParser
{
	String getName();

	String getUsage();

	String getHelloPrompt();

	boolean parseInput(String inLine, final List<Object> aResult) throws ParseException;
}

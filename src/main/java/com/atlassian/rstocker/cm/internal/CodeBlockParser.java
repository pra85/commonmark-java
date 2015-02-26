package com.atlassian.rstocker.cm.internal;

import com.atlassian.rstocker.cm.node.Block;
import com.atlassian.rstocker.cm.node.CodeBlock;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.atlassian.rstocker.cm.internal.Common.unescapeString;

public class CodeBlockParser extends AbstractBlockParser {

	private static final Pattern CLOSING_FENCE = Pattern.compile("^(?:`{3,}|~{3,})(?= *$)");
	private static final Pattern TRAILING_BLANK_LINES = Pattern.compile("(?:\n[ \t]*)+$");

	private final CodeBlock block = new CodeBlock();
	private final BlockContent content = new BlockContent();

	public CodeBlockParser(char fenceChar, int fenceLength, int fenceOffset) {
		block.setFenceChar(fenceChar);
		block.setFenceLength(fenceLength);
		block.setFenceOffset(fenceOffset);
	}

	public CodeBlockParser() {
	}

	@Override
	public ContinueResult continueBlock(String line, int nextNonSpace, int[] offset, boolean blank) {
		int indent = nextNonSpace - offset[0];
		if (block.isFenced()) { // fenced
			Matcher matcher = null;
			boolean matches = (indent <= 3 &&
					nextNonSpace < line.length() &&
					line.charAt(nextNonSpace) == block.getFenceChar() &&
					(matcher = CLOSING_FENCE.matcher(line.substring(nextNonSpace)))
							.find());
			if (matches && matcher.group(0).length() >= block.getFenceLength()) {
				// closing fence - we're at end of line, so we can finalize now
				return ContinueResult.FINALIZE;
			} else {
				// skip optional spaces of fence offset
				int i = block.getFenceOffset();
				while (i > 0 && offset[0] < line.length() && line.charAt(offset[0]) == ' ') {
					offset[0]++;
					i--;
				}
			}
		} else { // indented
			if (indent >= DocumentParser.CODE_INDENT) {
				offset[0] += DocumentParser.CODE_INDENT;
			} else if (blank) {
				offset[0] = nextNonSpace;
			} else {
				return ContinueResult.NOT_MATCHED;
			}
		}
		return ContinueResult.MATCHED;
	}

	@Override
	public boolean acceptsLine() {
		return true;
	}

	@Override
	public void addLine(String line) {
		content.add(line);
	}

	@Override
	public void finalizeBlock(InlineParser inlineParser) {
		boolean singleLine = content.hasSingleLine();
		// add trailing newline
		content.add("");
		String contentString = content.getString();

		if (block.isFenced()) { // fenced
			// first line becomes info string
			int firstNewline = contentString.indexOf('\n');
			String firstLine = contentString.substring(0, firstNewline);
			block.setInfo(unescapeString(firstLine.trim()));
			if (singleLine) {
				block.setLiteral("");
			} else {
				String literal = contentString.substring(firstNewline + 1);
				block.setLiteral(literal);
			}
		} else { // indented
			String literal = TRAILING_BLANK_LINES.matcher(contentString).replaceFirst("\n");
			block.setLiteral(literal);
		}
	}

	@Override
	public Block getBlock() {
		return block;
	}
}

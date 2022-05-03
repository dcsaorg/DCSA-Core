package org.dcsa.core.extendedrequest;

import org.dcsa.core.exception.ConcreteRequestErrorMessageException;

enum ParseState {
  INITIAL,
  PARSING_CURSOR,
  PARSED_CURSOR,
  PARSING_ARGUMENTS,
  PARSED_ARGUMENTS,
  PARSING_DEFAULTS,
  PARSED_DEFAULT,
  END,
  ;

  public ParseState parsedArgument(String cursorParameterName) {
    return switch (this) {
      case PARSED_CURSOR -> throw ConcreteRequestErrorMessageException.invalidQuery(cursorParameterName,
        "Cursor cannot be combined with other parameters");
      case INITIAL -> PARSING_ARGUMENTS;
      case PARSING_DEFAULTS, PARSING_ARGUMENTS, PARSING_CURSOR -> this;
      default -> throw ConcreteRequestErrorMessageException.internalServerError(
        "Attempt to parse arguments from an invalid state: " + this);
    };
  }

  public ParseState parsingCursor(String cursorParameterName) {
    if (this == PARSING_ARGUMENTS) {
      throw ConcreteRequestErrorMessageException.invalidQuery(cursorParameterName,
        "Cursor cannot be combined with other parameters");
    }
    if (this != INITIAL) {
      throw ConcreteRequestErrorMessageException.internalServerError("Attempt to parse cursor from an invalid state: " + this);
    }
    return PARSING_CURSOR;
  }

  public ParseState finishParsingRound() {
    return switch (this) {
      // INITIAL appears here if there were no arguments at all.  State-wise, that means we
      // are done parsing arguments.
      case INITIAL, PARSING_ARGUMENTS -> PARSED_ARGUMENTS;
      case PARSING_CURSOR -> PARSED_CURSOR;
      case PARSING_DEFAULTS -> PARSED_DEFAULT;
      default -> throw ConcreteRequestErrorMessageException.internalServerError(
        "Invalid state for finishing a parsing round: " + this);
    };
  }

  public ParseState startParseDefaultsRound() {
    return switch (this) {
      case INITIAL, PARSED_ARGUMENTS, PARSED_CURSOR -> PARSING_DEFAULTS;
      default -> throw ConcreteRequestErrorMessageException.internalServerError(
        "Invalid state for starting parsing of defaults: " + this);
    };
  }

  public ParseState endParsing() {
    return switch (this) {
      // END is
      case INITIAL, PARSED_CURSOR, PARSED_ARGUMENTS, PARSED_DEFAULT -> END;
      default -> throw ConcreteRequestErrorMessageException.internalServerError(
        "Invalid state for ending parsing: " + this);
    };
  }
}

package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import javax.naming.directory.InvalidAttributeIdentifierException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = null;
        try {
            expression = parseExpression();
        } catch (RuntimeException re) {
            // ignore :)
        }
        if (expression == null || tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    private List<Expression> parseArgumentList() {
        List<Expression> arguments = new LinkedList<>();
        do {
            Expression expression = parseExpression();
            arguments.add(expression);
        } while (tokens.matchAndConsume(COMMA));

        if (tokens.matchAndConsume(RIGHT_PAREN)) {
            return arguments;
        } else {
            return null;
        }
    }

    private Expression parseListLiteral() {
        if (tokens.match(LEFT_BRACKET)) {
            Token start = tokens.consumeToken();
            List<Expression> listItems = new LinkedList<>();
            if (tokens.match(RIGHT_BRACKET)) {
                Token end = tokens.consumeToken();
                ListLiteralExpression listLiteralExpression = new ListLiteralExpression(listItems);
                return listLiteralExpression;
            } else {
                do {
                    Expression expression = parseExpression();
                    listItems.add(expression);
                } while (tokens.matchAndConsume(COMMA));

                ListLiteralExpression listLiteralExpression = new ListLiteralExpression(listItems);
                require(RIGHT_BRACKET, listLiteralExpression, ErrorType.UNTERMINATED_LIST);
                return listLiteralExpression;
            }
        } else {
            return null;
        }
    }

    private Expression parseFunctionCall(Token identifier) {
        if (tokens.match(LEFT_PAREN)) {
            Token start = tokens.consumeToken();
            List<Expression> listItems = new LinkedList<>();
            if (tokens.match(RIGHT_PAREN)) {
                Token end = tokens.consumeToken();
                FunctionCallExpression functionCallExpression = new FunctionCallExpression(identifier.getStringValue(), listItems);
                return functionCallExpression;
            } else {
                do {
                    Expression expression = parseExpression();
                    listItems.add(expression);
                } while (tokens.matchAndConsume(COMMA));

                FunctionCallExpression functionCallExpression = new FunctionCallExpression(identifier.getStringValue(), listItems);
                require(RIGHT_PAREN, functionCallExpression, ErrorType.UNTERMINATED_ARG_LIST);
                return functionCallExpression;
            }
        } else {
            return null;
        }
    }


    //============================================================
    //  Statements
    //============================================================

    private FunctionDefinitionStatement parseFunctionDefinition() {
        if (tokens.match(FUNCTION)) {
            Token start = tokens.consumeToken();
            FunctionDefinitionStatement funcDef = new FunctionDefinitionStatement();
            funcDef.setStart(start);

            Token functionName = require(IDENTIFIER, funcDef);
            funcDef.setName(functionName.getStringValue());
            require(LEFT_PAREN, parsePrintStatement());
            if (!tokens.match(RIGHT_PAREN)) {
                do {
                    Token parameterName = require(IDENTIFIER, funcDef);
                    TypeLiteral typeLiteral = null;
                    if (tokens.matchAndConsume(COLON)) {
                        typeLiteral = parseTypeLiteral();

                    }
                    funcDef.addParameter(parameterName.getStringValue(), typeLiteral);
                } while (tokens.matchAndConsume(COMMA) && tokens.hasMoreTokens());
            }

            require(RIGHT_PAREN, parseStatement());
            TypeLiteral typeliteral = null;
            if(tokens.matchAndConsume(COLON)) {

            }
            LinkedList<Statement> statements = new LinkedList<>();
            currentFunctionDefinition = funcDef;
            while(!tokens.match(RIGHT_BRACE) && tokens.hasMoreTokens()){
                statements.add(parseStatement());
            }
            require(RIGHT_BRACE, funcDef);
        } else {
            return null;
        }
        return null;
    }

    private Statement parseStatement() {
        Statement stmt = parsePrintStatement();
        if (stmt != null)
        {
            return stmt;
        }
        stmt = parseForStatement();
        if(stmt != null)
        {
            return stmt;
        }
        stmt = parseIfStatement();
        if(stmt != null)
        {
            return stmt;
        }
        stmt = parseVarStatement();
        if(stmt != null)
        {
            return stmt;
        }
        stmt = parseAssignmentOrFunctionCallStatement();
        if(stmt != null)
        {
            return stmt;
        }
        if (currentFunctionDefinition != null)
        {
            stmt = parseReturnStatement();
            if(stmt != null)
            {
                return stmt;
            }
        }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parseForStatement() {
        if (tokens.match(FOR)) {
            ForStatement forStatement = new ForStatement();
            forStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, forStatement);
            forStatement.setVariableName(require(IDENTIFIER, forStatement).getStringValue());
            require(IN, forStatement);
            forStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, forStatement);

            require(LEFT_BRACE, forStatement);
            List<Statement> forStatements = new LinkedList<>();
            while (!tokens.match(RIGHT_BRACE) && tokens.hasMoreTokens()) {
                forStatements.add(parseStatement());
            }
            forStatement.setEnd(require(RIGHT_BRACE, forStatement));
            forStatement.setBody(forStatements);

            return forStatement;
        } else {
            return null;
        }
    }

    private Statement parseIfStatement() {
        if (tokens.match(IF)) {
            IfStatement ifStatement = new IfStatement();
            ifStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, ifStatement);
            ifStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, ifStatement);
            require(LEFT_BRACE, ifStatement);

            List<Statement> statements = new LinkedList<>();
            while (tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACE)) {
                Statement statement = parseStatement();
                statements.add(statement);
            }

            ifStatement.setTrueStatements(statements);
            require(RIGHT_BRACE, ifStatement);

            if (tokens.match(ELSE)) {
                List<Statement> elseStatements = new LinkedList<>();
                tokens.consumeToken();
                if(tokens.match(IF)) {
                    tokens.consumeToken();
                    parseIfStatement();
                } else {
                    require(LEFT_BRACE, ifStatement);
                    while (tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACE)) {
                        Statement elseStatement = parseStatement();
                        elseStatements.add(elseStatement);
                    }
                    ifStatement.setEnd(require(RIGHT_BRACE, ifStatement));
                    ifStatement.setElseStatements(elseStatements);
                }

                return ifStatement;
            }
            return ifStatement;
        } else {
            return null;
        }
    }

    private Statement parseVarStatement() {
        if (tokens.match(VAR)) {
            VariableStatement variableStatement = new VariableStatement();
            variableStatement.setStart(tokens.consumeToken());
            Token varName = require(IDENTIFIER, variableStatement);

            if (tokens.matchAndConsume(COLON)) {
                variableStatement.setExplicitType(parseTypeLiteral().getType());
            }

            require(EQUAL, variableStatement);

            variableStatement.setExpression(parseExpression());
            variableStatement.setVariableName(varName.getStringValue());
            return variableStatement;

        } else {
            return null;
        }
    }

    private Statement parseReturnStatement() {
        if(tokens.match(RETURN)) {
            ReturnStatement returnStatement = new ReturnStatement();
            returnStatement.setStart(tokens.consumeToken());
            returnStatement.setFunctionDefinition(currentFunctionDefinition);

            if (!tokens.match(RIGHT_BRACE)) {
                returnStatement.setExpression(parseExpression());
                return returnStatement;
            } else
            {
                return returnStatement;
            }
        } else
        {
            return null;
        }
    }

    private Statement parseAssignmentOrFunctionCallStatement() {
        if (tokens.match(IDENTIFIER)) {
            Token start = tokens.consumeToken();
            if (tokens.match(EQUAL))
            {
                tokens.consumeToken();
                final AssignmentStatement assignmentStmt = new AssignmentStatement();
                assignmentStmt.setStart(start);
                assignmentStmt.setVariableName(start.getStringValue());
                assignmentStmt.setExpression(parseExpression());
                return assignmentStmt;
            } else if (tokens.matchAndConsume(LEFT_PAREN)) {
                List<Expression> arguments = new LinkedList<>();
                while (!tokens.match(RIGHT_PAREN) && tokens.hasMoreTokens()){
                    if (tokens.match(COMMA)){
                        tokens.consumeToken();
                    } else {
                        Expression expression = parseExpression();
                        arguments.add(expression);
                    }
                }

                FunctionCallStatement functionCallStatement = new FunctionCallStatement(new FunctionCallExpression(start.getStringValue(), arguments));
                functionCallStatement.setStart(start);
                require(RIGHT_PAREN, functionCallStatement);
                return functionCallStatement;
            }
        }
        return null;
    }

    private FunctionCallExpression parseFunctionCallExpression(Token start)
    {
        if (tokens.match(LEFT_PAREN)) {
            tokens.consumeToken();
            if (tokens.match(RIGHT_PAREN)) {
                List<Expression> argumentList = parseArgumentList();
                FunctionCallExpression functionCallExpression = new FunctionCallExpression(start.getStringValue(), argumentList);
                return functionCallExpression;
            } else {
                List<Expression> argumentList = parseArgumentList();
                if (tokens.match(RIGHT_PAREN)) {
                    FunctionCallExpression functionCallExpression = new FunctionCallExpression(start.getStringValue(), argumentList);
                    return functionCallExpression;
                } else {
                    FunctionCallExpression functionCallExpression = new FunctionCallExpression(start.getStringValue(), argumentList);
                    functionCallExpression.addError(ErrorType.UNTERMINATED_ARG_LIST);
                    return functionCallExpression;
                }
            }
        } else {
            return null;
        }
    }


    private Statement parseProgramStatement() {
        Statement statement = parseFunctionDefinitionStatement();
        if (statement != null) {
            return statement;
        }
        return parseStatement();
    }

    private Statement parseFunctionDefinitionStatement()
    {
        if (tokens.match(FUNCTION)) {
            Token start = tokens.consumeToken();
            FunctionDefinitionStatement funcDef = new FunctionDefinitionStatement();
            funcDef.setStart(start);

            Token functionName = require(IDENTIFIER, funcDef);
            funcDef.setName(functionName.getStringValue());

            //parameters
            require(LEFT_PAREN, funcDef);
            if (!tokens.match(RIGHT_PAREN)) {
                do {
                    Token parameterName = require(IDENTIFIER, funcDef);
                    TypeLiteral typeLiteral = null;
                    if(tokens.matchAndConsume(COLON)) {
                        typeLiteral = parseTypeLiteral();
                    }
                    funcDef.addParameter(parameterName.getStringValue(), typeLiteral);
                } while (tokens.matchAndConsume(COMMA) && tokens.hasMoreTokens());
            }
            require(RIGHT_PAREN, funcDef);

            TypeLiteral returnType = null;
            if (tokens.matchAndConsume(COLON)) {
                returnType = parseTypeLiteral();
            }
            funcDef.setType(returnType);


            require(LEFT_BRACE, funcDef);
            LinkedList<Statement> statements = new LinkedList<>();
            currentFunctionDefinition = funcDef;
            while (!tokens.match(RIGHT_BRACE) && tokens.hasMoreTokens()) {
                statements.add(parseStatement());
            }
            currentFunctionDefinition = null;
            require(RIGHT_BRACE, funcDef);

            funcDef.setBody(statements);
            return funcDef;

        } else {
            return null;
        }
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }

    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression() {
        Expression expression = parseComparisonExpression();
        while (tokens.match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseComparisonExpression();
            EqualityExpression equalityExpression = new EqualityExpression(operator, expression, rightHandSide);
            equalityExpression.setStart(expression.getStart());
            equalityExpression.setEnd(rightHandSide.getEnd());
            expression = equalityExpression;
        }
        return expression;
    }

    private Expression parseTypeExpression(){
        TypeLiteral typeLiteral = new TypeLiteral();
        if(tokens.getCurrentToken().getStringValue().equals("bool"))
        {
            typeLiteral.setType(CatscriptType.BOOLEAN);
        } else if (tokens.getCurrentToken().getStringValue().equals("null"))
        {
            typeLiteral.setType(CatscriptType.NULL);
        } else if (tokens.getCurrentToken().getStringValue().equals("int"))
        {
            typeLiteral.setType(CatscriptType.INT);
        } else if (tokens.getCurrentToken().getStringValue().equals("object"))
        {
            typeLiteral.setType(CatscriptType.OBJECT);
        }
        return typeLiteral;
    }

    private Expression parseComparisonExpression() {
        Expression expression = parseAdditiveExpression();
        while (tokens.match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseAdditiveExpression();
            ComparisonExpression comparisonExpression = new ComparisonExpression(operator, expression, rightHandSide);
            comparisonExpression.setStart(expression.getStart());
            comparisonExpression.setEnd(rightHandSide.getEnd());
            expression = comparisonExpression;
        }
        return expression;
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
        }
        return expression;
    }

    private Expression parseFactorExpression() {
        Expression expression = parseUnaryExpression();
        while (tokens.match(SLASH, STAR)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator, expression, rightHandSide);
            factorExpression.setStart(expression.getStart());
            factorExpression.setEnd(rightHandSide.getEnd());
            expression = factorExpression;
        }
        return expression;
    }

    private Expression parseUnaryExpression() {
        if (tokens.match(MINUS, NOT)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        } else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        } else if (tokens.match(STRING)) {
            Token stringToken = tokens.consumeToken();
            StringLiteralExpression stringExpression = new StringLiteralExpression(stringToken.getStringValue());
            stringExpression.setToken(stringToken);
            return stringExpression;
        } else if (tokens.match(NULL)) {
            Token nullToken = tokens.consumeToken();
            NullLiteralExpression nullExpression = new NullLiteralExpression();
            nullExpression.setToken(nullToken);
            return nullExpression;
        } else if (tokens.match(FALSE)) {
            Token falseToken = tokens.consumeToken();
            BooleanLiteralExpression falseExpression = new BooleanLiteralExpression(false);
            falseExpression.setToken(falseToken);
            return falseExpression;
        } else if (tokens.match(TRUE)) {
            Token trueToken = tokens.consumeToken();
            BooleanLiteralExpression trueExpression = new BooleanLiteralExpression(true);
            trueExpression.setToken(trueToken);
            return trueExpression;
        } else if (tokens.match(IDENTIFIER)) {
            Token identifierToken = tokens.consumeToken();
            if (tokens.match(LEFT_PAREN)) {
                return parseFunctionCall(identifierToken);
            } else {
                IdentifierExpression identifierExpression = new IdentifierExpression(identifierToken.getStringValue());
                identifierExpression.setToken(identifierToken);
                return identifierExpression;
            }
        } else if (tokens.match(LEFT_BRACKET)) {
            return parseListLiteral();
        } else if (tokens.matchAndConsume(LEFT_PAREN)) {
            ParenthesizedExpression parenthesizedExpression = new ParenthesizedExpression(parseExpression());
            require(RIGHT_PAREN, parenthesizedExpression);
            return parenthesizedExpression;
        } else {
            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression(tokens.consumeToken());
            return syntaxErrorExpression;
        }
    }


    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if (tokens.match(type)) {
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

    private TypeLiteral parseTypeLiteral() {
        TypeLiteral typeLiteral = new TypeLiteral();
        if (tokens.match("int")) {
            typeLiteral.setType(CatscriptType.INT);
            typeLiteral.setToken(tokens.consumeToken());
            return typeLiteral;
        } else if (tokens.match("string")) {
            typeLiteral.setType(CatscriptType.STRING);
            typeLiteral.setToken(tokens.consumeToken());
            return typeLiteral;
        } else if (tokens.match("bool")) {
            typeLiteral.setType(CatscriptType.BOOLEAN);
            typeLiteral.setToken(tokens.consumeToken());
            return typeLiteral;
        } else if (tokens.match("object")) {
            typeLiteral.setType(CatscriptType.OBJECT);
            typeLiteral.setToken(tokens.consumeToken());
            return typeLiteral;
        } else if (tokens.getCurrentToken().getStringValue().equals("void")) {
            typeLiteral.setType(CatscriptType.VOID);
        } else if (tokens.getCurrentToken().getStringValue().equals("list")) {
            tokens.consumeToken();
            if (tokens.matchAndConsume(LESS)){
                typeLiteral.setType(new CatscriptType.ListType(parseTypeExpression().getType()));
                tokens.consumeToken();
            } else {
                typeLiteral.setType( new CatscriptType.ListType(CatscriptType.OBJECT));
                return typeLiteral;
            }
        } else {
            typeLiteral.setType(CatscriptType.OBJECT);
        }
        tokens.consumeToken();
        return typeLiteral;
    }
}
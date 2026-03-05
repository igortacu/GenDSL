lexer grammar ELSDLexer;

// ── Keywords: Types ──────────────────────────────────────────────────
GENE        : 'gene' ;
GENES       : 'genes' ;
PARENT      : 'parent' ;
GENERATION  : 'generation' ;
BOOLEAN     : 'boolean' ;
STRING_TYPE : 'string' ;
NUMBER_TYPE : 'number' ;

// ── Keywords: Flow Control ───────────────────────────────────────────
IF          : 'if' ;
THEN        : 'then' ;
ELSE        : 'else' ;
ELIF        : 'elif' ;
WHILE       : 'while' ;
DO          : 'do' ;
FOR         : 'for' ;
IN          : 'in' ;
END         : 'end' ;

// ── Keywords: Logic ──────────────────────────────────────────────────
AND         : 'and' ;
OR          : 'or' ;
NOT         : 'not' ;
TRUE        : 'true' ;
FALSE       : 'false' ;

// ── Keywords: Assignment / Fields ────────────────────────────────────
SET         : 'set' ;
DOM         : 'dom' ;
LABEL       : 'label' ;
PHENOTYPE   : 'phenotype' ;
GENOTYPE    : 'genotype' ;
CODOMINANCE : 'codominance' ;
LOCATION    : 'location' ;
LINKED      : 'linked' ;
SEXLINKED   : 'sexlinked' ;
AUTOSOMAL   : 'autosomal' ;
RATIO       : 'ratio' ;

// ── Keywords: Computations ───────────────────────────────────────────
FIND        : 'find' ;
CROSS       : 'cross' ;
PRED        : 'pred' ;
ESTIMATE    : 'estimate' ;
PRINT       : 'print' ;
ALL         : 'all' ;
INFER       : 'infer' ;
PARENTS     : 'parents' ;
FROM        : 'from' ;
PROBABILITY : 'probability' ;
GIVEN       : 'given' ;
CONFIDENCE  : 'confidence' ;
LINKAGE     : 'linkage' ;
RECOMBINATION : 'recombination' ;
DISTANCE    : 'distance' ;
BLOODGROUP  : 'bloodgroup' ;
SYSTEM      : 'system' ;
CARRIES     : 'carries' ;

// ── Keywords: Blood Systems ──────────────────────────────────────────
ABO         : 'ABO' ;
RH          : 'Rh' ;

// ── Operators ────────────────────────────────────────────────────────
PLUS        : '+' ;
MINUS       : '-' ;
STAR        : '*' ;
SLASH       : '/' ;
ASSIGN      : '=' ;
ARROW       : '->' ;
CROSS_OP    : 'x' ;

LT          : '<' ;
LE          : '<=' ;
GT          : '>' ;
GE          : '>=' ;
EQ          : '==' ;
NEQ         : '!=' ;

QUESTION    : '?' ;
COLON       : ':' ;

// ── Delimiters ───────────────────────────────────────────────────────
SEMI        : ';' ;
COMMA       : ',' ;
DOT         : '.' ;
LPAREN      : '(' ;
RPAREN      : ')' ;
LBRACK      : '[' ;
RBRACK      : ']' ;

// ── Literals ─────────────────────────────────────────────────────────
NUMBER
    : '-'? DIGIT+ ('.' DIGIT+)?
    ;

STRING_LITERAL
    : '"' (~["\r\n])* '"'
    ;

// ── Identifiers ──────────────────────────────────────────────────────
ID
    : LETTER (LETTER | DIGIT | '_')*
    ;

// ── Comments (skip) ──────────────────────────────────────────────────
LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;

// ── Whitespace (skip) ───────────────────────────────────────────────
WS
    : [ \t\r\n]+ -> skip
    ;

// ── Fragments ────────────────────────────────────────────────────────
fragment LETTER : [a-zA-Z] ;
fragment DIGIT  : [0-9] ;

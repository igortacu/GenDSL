parser grammar ELSDParser;

options { tokenVocab = ELSDLexer; }

// ═══════════════════════════════════════════════════════════════════════
// R1. program → [comment]* statement_list
// ═══════════════════════════════════════════════════════════════════════
program
    : statementList EOF
    ;

// ═══════════════════════════════════════════════════════════════════════
// R2. statement_list → statements [statement_list]
// ═══════════════════════════════════════════════════════════════════════
statementList
    : statement+
    ;

// ═══════════════════════════════════════════════════════════════════════
// R3. statements → declarations | assignments | flow_structures
//                | computations | io | comment
// ═══════════════════════════════════════════════════════════════════════
statement
    : declaration
    | assignment
    | flowStructure
    | computation
    | io
    ;

// ═══════════════════════════════════════════════════════════════════════
// R4. declarations → type id_list [= expression] ;
// R5. type → gene | genes | parent | generation | boolean | string | number
// ═══════════════════════════════════════════════════════════════════════
declaration
    : type idList (ASSIGN expression)? SEMI
    ;

type
    : GENE
    | GENES
    | PARENT
    | GENERATION
    | BOOLEAN
    | STRING_TYPE
    | NUMBER_TYPE
    ;

// ═══════════════════════════════════════════════════════════════════════
// R6–R7. id, id_list
// ═══════════════════════════════════════════════════════════════════════
idList
    : ID (COMMA ID)*
    ;

// ═══════════════════════════════════════════════════════════════════════
// R14. assignments →
//   set [field] id = expression ;
// | set [field] id = computations ;
// | set [field] id_list = expr_list ;
// | set dom : bigalpha -> smallalpha ;
// ═══════════════════════════════════════════════════════════════════════
assignment
    : SET field? ID ASSIGN expression SEMI                      # assignExpr
    | SET field? ID ASSIGN computation                          # assignComputation
    | SET field? idList ASSIGN exprList SEMI                    # assignMulti
    | SET DOM COLON ID ARROW ID SEMI                            # assignDominance
    ;

// ═══════════════════════════════════════════════════════════════════════
// R15. field → label | dom | phenotype | genotype | codominance
//            | location | linked | sexlinked | autosomal | ratio
// ═══════════════════════════════════════════════════════════════════════
field
    : LABEL
    | DOM
    | PHENOTYPE
    | GENOTYPE
    | CODOMINANCE
    | LOCATION
    | LINKED
    | SEXLINKED
    | AUTOSOMAL
    | RATIO
    ;

// ═══════════════════════════════════════════════════════════════════════
// R16. flow_structures →
//   if condition then statement_list [elif condition then statement_list]*
//       [else statement_list] end ;
// | condition ? statements : statements ;
// | while condition do statement_list end ;
// | for id in expr_list do statement_list end ;
// ═══════════════════════════════════════════════════════════════════════
flowStructure
    : IF condition THEN statementList
          (ELIF condition THEN statementList)*
          (ELSE statementList)?
      END SEMI                                                  # ifStatement
    | condition QUESTION statement COLON statement SEMI          # ternaryStatement
    | WHILE condition DO statementList END SEMI                  # whileStatement
    | FOR ID IN exprList DO statementList END SEMI               # forStatement
    ;

// ═══════════════════════════════════════════════════════════════════════
// R17. condition → expression operator expression
//               | not condition
//               | condition and condition
//               | condition or condition
//               | ( condition )
// ═══════════════════════════════════════════════════════════════════════
condition
    : NOT condition                                              # condNot
    | condition AND condition                                    # condAnd
    | condition OR condition                                     # condOr
    | expression operator expression                             # condCompare
    | LPAREN condition RPAREN                                    # condParen
    ;

// ═══════════════════════════════════════════════════════════════════════
// R24. operator → < | > | <= | >= | == | != | and | or
// ═══════════════════════════════════════════════════════════════════════
operator
    : LT
    | GT
    | LE
    | GE
    | EQ
    | NEQ
    | AND
    | OR
    ;

// ═══════════════════════════════════════════════════════════════════════
// R18. expression → number | string | true | false
//                 | id
//                 | event
//                 | unary_expr
//                 | binary_expr
//                 | ( expression )
// R19. unary_expr → - expression | not expression
// R20. binary_expr → expression (+|-|*|/) expression
// ═══════════════════════════════════════════════════════════════════════
expression
    : MINUS expression                                           # exprUnaryMinus
    | NOT expression                                             # exprNot
    | expression (STAR | SLASH) expression                       # exprMulDiv
    | expression (PLUS | MINUS) expression                       # exprAddSub
    | NUMBER                                                     # exprNumber
    | STRING_LITERAL                                             # exprString
    | TRUE                                                       # exprTrue
    | FALSE                                                      # exprFalse
    | event                                                      # exprEvent
    | ID                                                         # exprId
    | LPAREN expression RPAREN                                   # exprParen
    ;

// ═══════════════════════════════════════════════════════════════════════
// R21. expr_list → expression [, expr_list]
// ═══════════════════════════════════════════════════════════════════════
exprList
    : expression (COMMA expression)*
    ;

// ═══════════════════════════════════════════════════════════════════════
// R25. param_list → id [, param_list] | expression [, param_list]
// ═══════════════════════════════════════════════════════════════════════
paramList
    : (ID | expression) (COMMA (ID | expression))*
    ;

// ═══════════════════════════════════════════════════════════════════════
// R26. computations → find_expr | cross_expr | pred_expr | est_expr
//                   | infer_expr | prob_expr | link_expr | sex_expr
//                   | blood_expr
// ═══════════════════════════════════════════════════════════════════════
computation
    : findExpr
    | crossExpr
    | predExpr
    | estExpr
    | inferExpr
    | probExpr
    | linkExpr
    | sexExpr
    | bloodExpr
    ;

// ═══════════════════════════════════════════════════════════════════════
// R27. find_expr → find field id [gen_tag] ;
// ═══════════════════════════════════════════════════════════════════════
findExpr
    : FIND field ID genTag? SEMI
    ;

// ═══════════════════════════════════════════════════════════════════════
// R28. cross_expr → cross id x id -> id [ratio expr_list] ;
// ═══════════════════════════════════════════════════════════════════════
crossExpr
    : CROSS ID CROSS_OP ID ARROW ID (RATIO exprList)? SEMI
    ;

// ═══════════════════════════════════════════════════════════════════════
// R29. pred_expr → pred id_list [gen_tag] ;
// ═══════════════════════════════════════════════════════════════════════
predExpr
    : PRED idList genTag? SEMI
    ;

// ═══════════════════════════════════════════════════════════════════════
// R30. est_expr → estimate id number [confidence number] ;
// ═══════════════════════════════════════════════════════════════════════
estExpr
    : ESTIMATE ID NUMBER (CONFIDENCE NUMBER)? SEMI
    ;

// ═══════════════════════════════════════════════════════════════════════
// R31. infer_expr → infer parents from id [, id_list] ;
//                 | infer field from id [, id_list] ;
// ═══════════════════════════════════════════════════════════════════════
inferExpr
    : INFER PARENTS FROM ID (COMMA idList)? SEMI                # inferParents
    | INFER field FROM ID (COMMA idList)? SEMI                  # inferField
    ;

// ═══════════════════════════════════════════════════════════════════════
// R32. prob_expr → probability event ;
//               | probability event given event ;
//               | probability event given event_list ;
//               | probability event_list ;
// ═══════════════════════════════════════════════════════════════════════
probExpr
    : PROBABILITY event SEMI                                     # probSimple
    | PROBABILITY event GIVEN event SEMI                         # probConditionalSingle
    | PROBABILITY event GIVEN eventList SEMI                     # probConditionalMulti
    | PROBABILITY eventList SEMI                                 # probMulti
    ;

// ═══════════════════════════════════════════════════════════════════════
// R33. link_expr → linkage id , id recombination number ;
//               | linkage id_list recombination number [distance number] ;
// ═══════════════════════════════════════════════════════════════════════
linkExpr
    : LINKAGE ID COMMA ID RECOMBINATION NUMBER SEMI              # linkPair
    | LINKAGE idList RECOMBINATION NUMBER (DISTANCE NUMBER)? SEMI # linkMulti
    ;

// ═══════════════════════════════════════════════════════════════════════
// R34. sex_expr → sexlinked id ;
//              | sexlinked id field expression ;
// ═══════════════════════════════════════════════════════════════════════
sexExpr
    : SEXLINKED ID SEMI                                          # sexSimple
    | SEXLINKED ID field expression SEMI                         # sexWithField
    ;

// ═══════════════════════════════════════════════════════════════════════
// R35. blood_expr → bloodgroup id system bloodsys ;
//                 | bloodgroup id_list system bloodsys
//                       [phenotype expr_list] ;
// ═══════════════════════════════════════════════════════════════════════
bloodExpr
    : BLOODGROUP ID SYSTEM bloodsys SEMI                         # bloodSingle
    | BLOODGROUP idList SYSTEM bloodsys
          (PHENOTYPE exprList)? SEMI                             # bloodMulti
    ;

// ═══════════════════════════════════════════════════════════════════════
// R36. event → phenotype(id) | genotype(id) | carries(id, allele_list)
// ═══════════════════════════════════════════════════════════════════════
event
    : PHENOTYPE LPAREN ID RPAREN                                 # eventPhenotype
    | GENOTYPE LPAREN ID RPAREN                                  # eventGenotype
    | CARRIES LPAREN ID COMMA alleleList RPAREN                  # eventCarries
    ;

// ═══════════════════════════════════════════════════════════════════════
// R37. event_list → event [, event_list]
// ═══════════════════════════════════════════════════════════════════════
eventList
    : event (COMMA event)*
    ;

// ═══════════════════════════════════════════════════════════════════════
// R38–R39. allele, allele_list
// ═══════════════════════════════════════════════════════════════════════
allele
    : ID
    ;

alleleList
    : allele (COMMA allele)*
    ;

// ═══════════════════════════════════════════════════════════════════════
// R40. bloodsys → ABO | Rh
// ═══════════════════════════════════════════════════════════════════════
bloodsys
    : ABO
    | RH
    ;

// ═══════════════════════════════════════════════════════════════════════
// R41. label_opt → label string | ε
// ═══════════════════════════════════════════════════════════════════════
labelOpt
    : LABEL STRING_LITERAL
    | /* epsilon */
    ;

// ═══════════════════════════════════════════════════════════════════════
// R42. gen_tag → generation number | ε
// ═══════════════════════════════════════════════════════════════════════
genTag
    : GENERATION NUMBER
    ;

// ═══════════════════════════════════════════════════════════════════════
// R44. io → print id ;
//         | print field [id | all expression] ;
//         | print expr_list ;
//         | print event_list ;
// ═══════════════════════════════════════════════════════════════════════
io
    : PRINT ID SEMI                                              # printId
    | PRINT field (ID | ALL expression)? SEMI                    # printField
    | PRINT exprList SEMI                                        # printExprList
    | PRINT eventList SEMI                                       # printEventList
    ;

# Language Grammar Specification

## Program Composition
$$
\begin{align*}
\text{Program'} &\rightarrow (\text{DeclarationOrFunction} \mid \text{Statement})^* \\
\text{DeclarationOrFunction} &\rightarrow \text{TypeSpecifier} \; \text{IDENTIFIER} \; (\text{FunctionTail} \mid \text{DeclarationTail})
\end{align*}
$$

## Declarations & Specifications
$$
\begin{align*}
\text{Declaration} &\rightarrow [\textbf{const}] \; \text{TypeSpecifier} \; \text{InitDeclaratorList} \; \textbf{;} \\
\text{TypeSpecifier} &\rightarrow \textbf{int} \mid \textbf{float} \mid \textbf{double} \mid \textbf{char} \mid \textbf{void}
\end{align*}
$$

## Statement Constructs
$$
\begin{align*}
\text{Statement} &\rightarrow \text{CompoundStatement} \mid \text{ExpressionStatement} \mid \text{SelectionStatement} \mid \text{IterativeStatement} \mid \text{ReturnStatement} \\
\text{CompoundStatement} &\rightarrow \textbf{\{} \; (\text{Declaration} \mid \text{Statement})^* \; \textbf{\}} \\
\text{SelectionStatement} &\rightarrow \textbf{if} \; \textbf{(} \; \text{Expression} \; \textbf{)} \; \text{Statement} \; [\textbf{else} \; \text{Statement}] \\
\text{IterativeStatement} &\rightarrow \textbf{while} \; \textbf{(} \; \text{Expression} \; \textbf{)} \; \text{Statement} \mid \textbf{for} \; \textbf{(} \; [\text{Expression}] \; \textbf{;} \; [\text{Expression}] \; \textbf{;} \; [\text{Expression}] \; \textbf{)} \; \text{Statement} \\
\text{ReturnStatement} &\rightarrow \textbf{return} \; [\text{Expression}] \; \textbf{;}
\end{align*}
$$

## Expressions Hierarchy (Operator Precedence & Associativity)
$$
\begin{align*}
\text{Expression} &\rightarrow \text{AssignmentExpression} \\
\text{AssignmentExpression} &\rightarrow \text{IDENTIFIER} \; \textbf{=} \; \text{AssignmentExpression} \mid \text{LogicalOrExpression} \\
\text{LogicalOrExpression} &\rightarrow \text{LogicalAndExpression} \; (\textbf{||} \; \text{LogicalAndExpression})^* \\
\text{LogicalAndExpression} &\rightarrow \text{EqualityExpression} \; (\textbf{\&\&} \; \text{EqualityExpression})^* \\
\text{EqualityExpression} &\rightarrow \text{RelationalExpression} \; ((\textbf{==} \mid \textbf{!=}) \; \text{RelationalExpression})^* \\
\text{RelationalExpression} &\rightarrow \text{AdditiveExpression} \; ((\textbf{<} \mid \textbf{>} \mid \textbf{<=} \mid \textbf{>=}) \; \text{AdditiveExpression})^* \\
\text{AdditiveExpression} &\rightarrow \text{MultiplicativeExpression} \; ((\textbf{+} \mid \textbf{-}) \; \text{MultiplicativeExpression})^* \\
\text{MultiplicativeExpression} &\rightarrow \text{UnaryExpression} \; ((\textbf{*} \mid \textbf{/}) \; \text{UnaryExpression})^* \\
\text{UnaryExpression} &\rightarrow \textbf{!} \; \text{UnaryExpression} \mid \text{PrimaryExpression} \\
\text{PrimaryExpression} &\rightarrow \text{IDENTIFIER} \mid \text{LITERAL\_NUM} \mid \text{LITERAL\_CHAR} \mid \textbf{(} \; \text{Expression} \; \textbf{)}
\end{align*}
$$
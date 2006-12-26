//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.util.StringUtil;

/** Utility functions operating on a tree of nodes. */
public final class NodeUtil
{
    /** Find first node with a certain move number in main variation
        containing a given node.
        @return null if no such node exists.
    */
    public static ConstNode findByMoveNumber(ConstNode node, int moveNumber)
    {
        int maxMoveNumber = getMoveNumber(node) + getMovesLeft(node);
        if (moveNumber < 0 || moveNumber >  maxMoveNumber)
            return null;
        if (moveNumber <= getMoveNumber(node))
        {
            while (node.getFatherConst() != null
                   && (getMoveNumber(node) > moveNumber
                       || node.getMove() == null))
                node = node.getFatherConst();
        }
        else
        {
            while (node.getChildConst() != null
                   && getMoveNumber(node) < moveNumber)
                node = node.getChildConst();
        }
        return node;
    }

    /** Return first node of a given variation.
        Returns the root node node if variation string is empty,
        otherwise it returns the node that can be reached from the root node
        by taking the children defined by the integers in the variation string
        for nodes with more than one child.
        Returns null, if the variation string is invalid or does not specify
        a node in the given tree.
    */
    public static ConstNode findByVariation(ConstNode root, String variation)
    {
        if (variation.trim().equals(""))
            return root;
        String[] tokens = StringUtil.split(variation, '.');
        int[] n = new int[tokens.length];
        for (int i = 0; i < tokens.length; ++i)
        {
            try
            {
                n[i] = Integer.parseInt(tokens[i]) - 1;
                if (n[i] < 0)
                    return null;
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }
        ConstNode node = root;
        for (int i = 0; i < n.length; ++i)
        {
            while (node.getNumberChildren() <= 1)
            {
                node = node.getChildConst();
                if (node == null)
                    return null;
            }
            if (n[i] >= node.getNumberChildren())
                return null;
            node = node.getChildConst(n[i]);
        }
        return node;
    }

    public static boolean commentContains(ConstNode node, Pattern pattern)
    {
        String comment = node.getComment();
        return (comment != null && pattern.matcher(comment).find());
    }

    public static ConstNode findInComments(ConstNode node, Pattern pattern)
    {
        node = nextNode(node);
        while (node != null)
        {
            if (commentContains(node, pattern))
                return node;
            node = nextNode(node);
        }
        return null;
    }

    /** Get stones added and moves all as moves for a list of nodes.
        Calls NodeUtil.getAllAsMoves(Node node) for all nodes in a
        sequence of subsequent nodes.
    */
    public static ArrayList getAllAsMoves(ArrayList nodes)
    {
        ArrayList moves = new ArrayList();
        ArrayList nodeMoves = new ArrayList();
        for (int i = 0; i < nodes.size(); ++i)
        {
            Node node = (Node)nodes.get(i);
            nodeMoves.clear();
            getAllAsMoves(node, nodeMoves);
            moves.addAll(nodeMoves);
            assert(i == 0 || node.isChildOf((Node)nodes.get(i - 1)));
        }
        return moves;
    }

    /** Get stones added and moves all as moves.
        This function is for transmitting setup stones to Go engines
        that support only play commands.
        May include moves with color EMPTY for delete stones.
        Also may include a pass move at the end to make sure, that the
        right color is to move after executing all returned moves.
        No check is performed if the setup stones create a position
        with no-liberty blocks, in which case a play command would
        capture some stones.
        The order of the moves is: setup stones black, setup stones white,
        setup stones empty, node move, optional pass.
        However, if the node contains setup stones of both colors and has a
        player set, the order of black and white setup stones is switched, if
        that avoids the pass move at the end.
        @param node the node
        @param moves the resulting moves
    */
    public static void getAllAsMoves(ConstNode node, ArrayList moves)
    {
        moves.clear();
        Move move = node.getMove();
        if (node.hasSetup())
        {
            int numberAddBlack = node.getNumberAddBlack();
            int numberAddWhite = node.getNumberAddWhite();
            boolean switchSetup = (move == null
                                   && node.getPlayer() == GoColor.WHITE
                                   && numberAddBlack > 0
                                   && numberAddWhite > 0);
            if (switchSetup)
            {
                for (int i = 0; i < numberAddWhite; ++i)
                    moves.add(Move.get(node.getAddWhite(i), GoColor.WHITE));
                for (int i = 0; i < numberAddBlack; ++i)
                    moves.add(Move.get(node.getAddBlack(i), GoColor.BLACK));
            }
            else
            {
                for (int i = 0; i < numberAddBlack; ++i)
                    moves.add(Move.get(node.getAddBlack(i), GoColor.BLACK));
                for (int i = 0; i < numberAddWhite; ++i)
                    moves.add(Move.get(node.getAddWhite(i), GoColor.WHITE));
            }
            for (int i = 0; i < node.getNumberAddEmpty(); ++i)
                moves.add(Move.get(node.getAddEmpty(i), GoColor.EMPTY));
        }
        if (move != null)
            moves.add(move);
        if (moves.size() > 0)
        {
            GoColor toMove = node.getToMove();
            Move lastMove = (Move)moves.get(moves.size() - 1);
            GoColor otherColor = lastMove.getColor().otherColor();
            if (toMove != GoColor.EMPTY && toMove != otherColor
                && otherColor != GoColor.EMPTY)
                moves.add(Move.getPass(otherColor));
        }
    }

    /** Find the last node that was still in the main variation. */
    public static ConstNode getBackToMainVariation(ConstNode node)
    {
        while (! isInMainVariation(node))
            node = node.getFatherConst();
        return node;
    }

    /** Get all children moves.
        @return ArrayList contaning the move points, not including passes
        and independent of color.
    */
    public static ArrayList getChildrenMoves(ConstNode node)
    {
        ArrayList moves = new ArrayList();
        for (int i = 0; i < node.getNumberChildren(); ++i)
        {
            Move childMove = node.getChildConst(i).getMove();
            if (childMove != null && childMove.getPoint() != null)
                moves.add(childMove.getPoint());
        }
        return moves;
    }

    /** Get child node containg a certain move.
        @return null if no such child existst.
    */
    public static ConstNode getChildWithMove(ConstNode node, Move move)
    {
        for (int i = 0; i < node.getNumberChildren(); ++i)
        {
            ConstNode child = node.getChildConst(i);
            Move childMove = child.getMove();
            if (childMove != null && childMove.equals(move))
                return child;
        }
        return null;
    }

    public static int getDepth(ConstNode node)
    {
        int depth = 0;
        while (node.getFatherConst() != null)
        {
            node = node.getFatherConst();
            ++depth;
        }
        return depth;
    }

    /** Get last node in main variation. */
    public static ConstNode getLast(ConstNode node)
    {
        while (node.getNumberChildren() > 0)
            node = node.getChildConst();
        return node;
    }

    public static int getMoveNumber(ConstNode node)
    {
        int moveNumber = 0;
        while (node != null)
        {
            if (node.getMove() != null)
                ++moveNumber;
            node = node.getFatherConst();
        }
        return moveNumber;
    }

    /** Moves left in main variation. */
    public static int getMovesLeft(ConstNode node)
    {
        int movesLeft = 0;
        node = node.getChildConst();
        while (node != null)
        {
            if (node.getMove() != null)
                ++movesLeft;
            node = node.getChildConst();
        }
        return movesLeft;
    }

    /** Return next variation of this node. */
    public static ConstNode getNextVariation(ConstNode node)
    {
        ConstNode father = node.getFatherConst();
        if (father == null)
            return null;
        return father.variationAfter(node);
    }

    /** Return next variation before this node. */
    public static ConstNode getNextEarlierVariation(ConstNode node)
    {
        ConstNode child = node;
        node = node.getFatherConst();
        while (node != null && node.variationAfter(child) == null)
        {
            child = node;
            node = node.getFatherConst();
        }
        if (node == null)
            return null;
        return node.variationAfter(child);
    }

    /** Nodes left in main variation. */
    public static int getNodesLeft(ConstNode node)
    {
        int nodesLeft = 0;
        while (node != null)
        {
            ++nodesLeft;
            node = node.getChildConst();
        }
        return nodesLeft;
    }

    /** Get nodes in path a given node to the root node. */
    public static void getPathToRoot(ConstNode node, ArrayList result)
    {
        result.clear();
        while (node != null)
        {
            result.add(node);
            node = node.getFatherConst();
        }
    }

    /** Return previous variation of this node. */
    public static ConstNode getPreviousVariation(ConstNode node)
    {
        ConstNode father = node.getFatherConst();
        if (father == null)
            return null;
        return father.variationBefore(node);
    }

    /** Return previous variation before this node. */
    public static ConstNode getPreviousEarlierVariation(ConstNode node)
    {
        ConstNode child = node;
        node = node.getFatherConst();
        while (node != null && node.variationBefore(child) == null)
        {
            child = node;
            node = node.getFatherConst();
        }
        if (node == null)
            return null;
        node = node.variationBefore(child);
        if (node == null)
            return null;
        while (hasSubtree(node))
            node = node.getChildConst(node.getNumberChildren() - 1);
        return node;
    }

    public static ConstNode getRoot(ConstNode node)
    {
        while (node.getFatherConst() != null)
            node = node.getFatherConst();
        return node;
    }

    /** Get a text representation of the variation to a certain node.
        The string contains the number of the child for each node with more
        than one child in the path from the root node to this node.
        The childs are counted starting with 1 and the numbers are separated
        by colons.
    */
    public static String getVariationString(ConstNode node)
    {
        ArrayList list = new ArrayList();
        while (node != null)
        {
            ConstNode father = node.getFatherConst();
            if (father != null && father.getNumberChildren() > 1)
            {
                int index = father.getChildIndex(node) + 1;
                list.add(0, Integer.toString(index));
            }
            node = father;
        }
        StringBuffer result = new StringBuffer(list.size() * 3);
        for (int i = 0; i < list.size(); ++i)
        {
            result.append((String)list.get(i));
            if (i < list.size() - 1)
                result.append('.');
        }
        return result.toString();
    }

    /** Subtree of node contains at least one node with 2 or more children. */
    public static boolean hasSubtree(ConstNode node)
    {
        while (node != null && node.getNumberChildren() < 2)
            node = node.getChildConst();
        return (node != null);
    }

    /** Check if game is in cleanup stage.
        Cleanup stage is after two consecutive pass moves have been played.
    */
    public static boolean isInCleanup(ConstNode node)
    {
        boolean lastPass = false;
        while (node != null)
        {
            Move move = node.getMove();
            if (move != null)
            {
                if (move.getPoint() == null)
                {
                    if (lastPass)
                        return true;
                    lastPass = true;
                }
                else
                    lastPass = false;
            }
            node = node.getFatherConst();
        }
        return false;
    }

    public static boolean isInMainVariation(ConstNode node)
    {
        while (node.getFatherConst() != null)
        {
            if (node.getFatherConst().getChildConst(0) != node)
                return false;
            node = node.getFatherConst();
        }
        return true;
    }

    public static boolean isRootWithoutChildren(ConstNode node)
    {
        return (node.getFatherConst() == null && node.getChildConst() == null);
    }

    public static void makeMainVariation(Node node)
    {
        while (node.getFatherConst() != null)
        {
            node.getFather().makeMainVariation(node);
            node = node.getFather();
        }
    }

    /** Create a game tree with the current board position as setup stones. */
    public static GameTree makeTreeFromPosition(ConstGameInformation info,
                                                ConstBoard board)
    {
        GameTree tree = new GameTree(board.getSize(), info.getKomi(), null,
                                     info.getRules(), info.getTimeSettings());
        Node root = tree.getRoot();
        for (int i = 0; i < board.getNumberPoints(); ++i)
        {
            GoPoint point = board.getPoint(i);
            GoColor color = board.getColor(point);
            if (color == GoColor.BLACK)
                root.addBlack(point);
            else if (color == GoColor.WHITE)
                root.addWhite(point);
        }
        root.setPlayer(board.getToMove());
        return tree;
    }

    /** Get next node for iteration in complete tree. */
    public static ConstNode nextNode(ConstNode node)
    {
        ConstNode child = node.getChildConst();
        if (child != null)
            return child;
        return getNextEarlierVariation(node);
    }

    /** Get next node for iteration in subtree. */
    public static ConstNode nextNode(ConstNode node, int depth)
    {
        node = nextNode(node);
        if (node == null || NodeUtil.getDepth(node) <= depth)
            return null;
        return node;
    }

    public static String nodeInfo(ConstNode node)
    {
        StringBuffer buffer = new StringBuffer(128);
        buffer.append("NodeProperties:\n");
        appendInfo(buffer, "Depth", getDepth(node));
        appendInfo(buffer, "Children", node.getNumberChildren());
        if (node.getMove() != null)
        {
            appendInfo(buffer, "Move", node.getMove().toString());
            appendInfo(buffer, "MoveNumber", getMoveNumber(node));
        }
        appendInfo(buffer, "Variation", getVariationString(node));
        ArrayList addBlack = new ArrayList();
        for (int i = 0; i < node.getNumberAddBlack(); ++i)
            addBlack.add(node.getAddBlack(i));
        if (node.getNumberAddBlack() > 0)
            appendInfo(buffer, "AddBlack", addBlack);
        ArrayList addWhite = new ArrayList();
        for (int i = 0; i < node.getNumberAddWhite(); ++i)
            addWhite.add(node.getAddWhite(i));
        if (node.getNumberAddWhite() > 0)
            appendInfo(buffer, "AddWhite", addWhite);
        ArrayList addEmpty = new ArrayList();
        for (int i = 0; i < node.getNumberAddEmpty(); ++i)
            addEmpty.add(node.getAddEmpty(i));
        if (node.getNumberAddEmpty() > 0)
            appendInfo(buffer, "AddEmpty", addEmpty);
        if (node.getPlayer() != GoColor.EMPTY)
            appendInfo(buffer, "Player", node.getPlayer().toString());
        if (! Double.isNaN(node.getTimeLeft(GoColor.BLACK)))
            appendInfo(buffer, "TimeLeftBlack",
                       node.getTimeLeft(GoColor.BLACK));
        if (node.getMovesLeft(GoColor.BLACK) >= 0)
            appendInfo(buffer, "MovesLeftBlack",
                       node.getMovesLeft(GoColor.BLACK));
        if (! Double.isNaN(node.getTimeLeft(GoColor.WHITE)))
            appendInfo(buffer, "TimeLeftWhite",
                       node.getTimeLeft(GoColor.WHITE));
        if (node.getMovesLeft(GoColor.WHITE) >= 0)
            appendInfo(buffer, "MovesLeftWhite",
                       node.getMovesLeft(GoColor.WHITE));
        appendInfoComment(buffer, node);
        for (int i = 0; i < MarkType.getNumberTypes(); ++i)
        {
            MarkType type = MarkType.getType(i);
            ArrayList marked = node.getMarkedConst(type);
            if (marked != null && marked.size() > 0)
                appendInfo(buffer, "Marked " +
                           StringUtil.capitalize(type.toString()), marked);
        }
        Map labels = node.getLabelsConst();
        if (labels != null && labels.size() > 0)
        {
            StringBuffer labelsBuffer = new StringBuffer();
            Iterator iter = labels.entrySet().iterator();
            while (iter.hasNext())
            {
                Map.Entry entry = (Map.Entry)iter.next();
                GoPoint point = (GoPoint)entry.getKey();
                String value = (String)entry.getValue();
                labelsBuffer.append(point);
                labelsBuffer.append(':');
                labelsBuffer.append(value);
                if (iter.hasNext())
                    labelsBuffer.append(' ');
            }
            appendInfo(buffer, "Labels", labelsBuffer.toString());
        }
        if (! Float.isNaN(node.getValue()))
            appendInfo(buffer, "Value", Float.toString(node.getValue()));
        Map sgfProperties = node.getSgfPropertiesConst();
        if (sgfProperties != null)
        {
            buffer.append("SgfProperties:\n");
            Iterator it = sgfProperties.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry entry = (Map.Entry)it.next();
                String label = (String)entry.getKey();
                String value = (String)entry.getValue();
                appendInfo(buffer, label, value);
            }
        }
        return buffer.toString();
    }
        
    public static boolean subtreeGreaterThan(ConstNode node, int size)
    {
        int n = 0;
        int depth = NodeUtil.getDepth(node);
        while (node != null)
        {
            ++n;
            if (n > size)
                return true;
            node = nextNode(node, depth);
        }
        return false;
    }

    /** Number of nodes in subtree.
        Does not include this node.
    */
    public static int subtreeSize(ConstNode node)
    {
        int n = 0;
        int depth = NodeUtil.getDepth(node);
        while (node != null)
        {
            ++n;
            node = nextNode(node, depth);
        }
        return n;
    }

    public static String treeInfo(ConstNode node)
    {
        int numberNodes = 0;
        int numberTerminal = 0;
        int moreThanOneChild = 0;
        int maxDepth = 0;
        int maxChildren = 0;
        double averageDepth = 0;
        double averageChildren = 0;
        double averageChildrenInner = 0;
        int rootDepth = getDepth(node);
        while (node != null)
        {
            ++numberNodes;
            int numberChildren = node.getNumberChildren();
            int depth = getDepth(node) - rootDepth;
            assert(depth >= 0);
            if (depth > maxDepth)
                maxDepth = depth;
            if (numberChildren > maxChildren)
                maxChildren = numberChildren;
            if (numberChildren == 0)
                ++numberTerminal;
            else
                averageChildrenInner += numberChildren;
            if (numberChildren > 1)
                ++moreThanOneChild;
            averageDepth += depth;
            averageChildren += numberChildren;
            node = nextNode(node, rootDepth);
        }
        int numberInner = numberNodes - numberTerminal;
        averageDepth /= numberNodes;
        averageChildren /= numberNodes;
        averageChildrenInner /= Math.max(numberInner, 1);
        NumberFormat format = StringUtil.getNumberFormat(3);
        format.setMinimumFractionDigits(3);
        StringBuffer buffer = new StringBuffer();
        appendInfo(buffer, "Nodes", numberNodes);
        appendInfo(buffer, "Terminal", numberTerminal);
        appendInfo(buffer, "Inner", numberInner);
        appendInfo(buffer, "AvgDepth", format.format(averageDepth));
        appendInfo(buffer, "MaxDepth", maxDepth);
        appendInfo(buffer, "AvgChildren", format.format(averageChildren));
        appendInfo(buffer, "AvgChildrenInner",
                   format.format(averageChildrenInner));
        appendInfo(buffer, "MaxChildren", maxChildren);
        appendInfo(buffer, "MoreThanOneChild", moreThanOneChild);
        return buffer.toString();
    }

    /** Remove all children. */
    public static void truncateChildren(Node node)
    {
        while (true)
        {
            Node child = node.getChild();
            if (child == null)
                break;
            node.removeChild(child);
        }
    }

    private static void appendInfo(StringBuffer buffer, String label,
                                   int value)
    {
        appendInfo(buffer, label, Integer.toString(value));
    }

    private static void appendInfo(StringBuffer buffer, String label,
                                   double value)
    {
        appendInfo(buffer, label, Double.toString(value));
    }

    private static void appendInfo(StringBuffer buffer, String label,
                                   ArrayList points)
    {
        appendInfoLabel(buffer, label);
        for (int i = 0; i < points.size(); ++i)
        {
            if (i % 10 == 9 && i < points.size() - 1)
            {
                buffer.append('\n');
                appendInfoLabel(buffer, "");
            }
            buffer.append((GoPoint)points.get(i));
            buffer.append(' ');
        }
        buffer.append('\n');
    }

    private static void appendInfo(StringBuffer buffer, String label,
                                   String value)
    {
        appendInfoLabel(buffer, label);
        buffer.append(value);
        buffer.append('\n');
    }

    private static void appendInfoComment(StringBuffer buffer, ConstNode node)
    {
        String comment = node.getComment();
        if (comment == null)
            return;
        boolean trimmed = false;
        int pos = comment.indexOf("\n");
        if (pos >= 0)
        {
            comment = comment.substring(0, pos);
            trimmed = true;
        }
        final int maxCharDisplayed = 30;
        if (comment.length() > maxCharDisplayed)
        {
            comment = comment.substring(0, maxCharDisplayed);
            trimmed = true;
        }
        if (trimmed)
            comment = comment + "...";
        appendInfo(buffer, "Comment", comment);
    }

    private static void appendInfoLabel(StringBuffer buffer, String label)
    {
        buffer.append(label);
        int numberEmpty = Math.max(0, 20 - label.length());
        for (int i = 0; i < numberEmpty; ++i)
            buffer.append(' ');
        buffer.append(' ');
    }

    /** Make constructor unavailable; class is for namespace only. */
    private NodeUtil()
    {
    }
}

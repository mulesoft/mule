/*
 * Compare two working copies after reintegrating an SVN feature branch.
 *
 * This script is only one step in the reintegration process:
 * 1) update your working copy of the target branch. Note down the revision (just to be safe)
 * 2) svnmerge up to the revision you noted into the working copy of the feature branch. commit.
 * 3) run mvn clean eclipse:clean on both working copies to avoid comparing unrelated files
 * 4) re-integrate the feature branch into your main branch working copy
 * 5) resolve any merge conflicts
 * 6) run diff -EbBr -x '.svn' <feature branch WC> <re-integrated WC> and capture the output
 * 7) feed the output through this script
 *
 * This script filters out all diffs that consist only of a change in the $Id$ keyword
 * 
 * $Id$
 */

if (args.length == 0)
{
    usage()
}

file = new File(args[0])
removedBlock = new Block()
insertedBlock = new Block()
diffedFile = null
file.eachLine
{
    line ->

    if (line.startsWith("diff"))
    {
        compareDifference()

        diffedFile = line
        removedBlock = new Block()
        insertedBlock = new Block()
    }
    else if (line.startsWith("<"))
    {
        removedBlock.addLine(line)
    }
    else if (line.startsWith(">"))
    {
        insertedBlock.addLine(line)
    }
}
compareDifference()

def compareDifference()
{
    if (diffedFile == null)
    {
        return
    }

    if (!removedBlockMatchesSvnId() || !insertedBlockMatchesSvnId())
    {
        println(diffedFile)
        println(removedBlock.content)
        println(insertedBlock.content)
    }
}

def removedBlockMatchesSvnId()
{
    if (removedBlock.lineCount > 1)
    {
        return false
    }

    // regex matches '< * $Id: filename revision date author $'
    matcher = removedBlock.content =~ /<\s+\*\s+\$[I]d:.*\$\n/
    return matcher.matches()
}

def insertedBlockMatchesSvnId()
{
    if (insertedBlock.lineCount > 1)
    {
        return false
    }

    // regex matches '>  * $Id: filename revision date author $'
    matcher = insertedBlock.content =~ />\s+\*\s+\$[I]d:.*\$\n/
    return matcher.matches()
}

def usage()
{
    println("Usage: CheckReintegratedWorkingCopy <diff output>")
    System.exit(1)
}

class Block
{
    String content
    int lineCount

    Block()
    {
        content = ""
        lineCount = 0
    }

    void addLine(String line)
    {
        content = content + line + "\n"
        lineCount++;
    }
}

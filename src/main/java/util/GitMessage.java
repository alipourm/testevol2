package util;


import org.eclipse.jgit.revwalk.RevCommit;

public class GitMessage {
    public static Stemmer stemmer = null;
    private static String[] keywords = {
            "error", "bug", "issue", "mistake", "incorrect", "fault", "defect"};



    public static boolean isBugFix(RevCommit revCommit){
        if (stemmer == null)
            stemmer = new Stemmer();


        String msg = revCommit.getFullMessage();
        for (String keyword: keywords)
            if (msg.contains(keyword))
                return true;

        return false;






    }
}

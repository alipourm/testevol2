package util;


import org.eclipse.jgit.revwalk.RevCommit;

public class GitMessage {
    private static String[] keywords = {
            "error", "bug", "issue", "mistake", "incorrect", "fault", "defect", "address an issue", "fix", "fixed"};

    public static boolean isBugFix(RevCommit revCommit){
        String msg = revCommit.getFullMessage().toLowerCase();
        for (String keyword: keywords)
            if (msg.contains(keyword))
                return true;
        return false;
    }
}

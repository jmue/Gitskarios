package com.alorma.github.ui.adapter.commit;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alorma.github.R;
import com.alorma.github.emoji.EmojiBitmapLoader;
import com.alorma.github.sdk.bean.dto.response.Commit;
import com.alorma.github.sdk.bean.dto.response.User;
import com.alorma.github.sdk.bean.info.RepoInfo;
import com.alorma.github.ui.adapter.base.RecyclerArrayAdapter;
import com.alorma.github.utils.AttributesUtils;
import com.alorma.github.utils.TextUtils;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.octicons_typeface_library.Octicons;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Bernat on 07/09/2014.
 */
public class CommitsAdapter extends RecyclerArrayAdapter<Commit, CommitsAdapter.ViewHolder> implements StickyRecyclerHeadersAdapter<CommitsAdapter.HeaderViewHolder> {

    private boolean shortMessage;
    private RepoInfo repoInfo;
    private CommitsAdapterListener commitsAdapterListener;
    private EmojiBitmapLoader emojiBitmapLoader;

    public CommitsAdapter(LayoutInflater inflater, boolean shortMessage, RepoInfo repoInfo) {
        super(inflater);
        this.shortMessage = shortMessage;
        this.repoInfo = repoInfo;
        emojiBitmapLoader = new EmojiBitmapLoader();
    }

    @Override
    public CommitsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(getInflater().inflate(R.layout.commit_row, parent, false));
    }

    @Override
    public void onBindViewHolder(CommitsAdapter.ViewHolder holder, Commit commit) {

        User author = commit.author;

        if (author == null) {
            author = commit.commit.author;
        }

        if (author == null) {
            author = commit.commit.committer;
        }

        if (author != null) {
            if (author.avatar_url != null) {
                ImageLoader.getInstance().displayImage(author.avatar_url, holder.avatar);
            } else if (author.email != null) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    digest.update(author.email.getBytes());
                    byte messageDigest[] = digest.digest();
                    StringBuffer hexString = new StringBuffer();
                    for (int i = 0; i < messageDigest.length; i++)
                        hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
                    String hash = hexString.toString();
                    ImageLoader.getInstance().displayImage("http://www.gravatar.com/avatar/" + hash, holder.avatar);
                } catch (NoSuchAlgorithmException e) {
                    IconicsDrawable iconDrawable = new IconicsDrawable(holder.itemView.getContext(), Octicons.Icon.oct_octoface);
                    iconDrawable.color(AttributesUtils.getSecondaryTextColor(holder.itemView.getContext()));
                    iconDrawable.sizeDp(36);
                    iconDrawable.setAlpha(128);
                    holder.avatar.setImageDrawable(iconDrawable);
                }

            } else {
                IconicsDrawable iconDrawable = new IconicsDrawable(holder.itemView.getContext(), Octicons.Icon.oct_octoface);
                iconDrawable.color(AttributesUtils.getSecondaryTextColor(holder.itemView.getContext()));
                iconDrawable.sizeDp(36);
                iconDrawable.setAlpha(128);
                holder.avatar.setImageDrawable(iconDrawable);
            }

            if (author.login != null) {
                holder.user.setText(author.login);
            } else if (author.name != null) {
                holder.user.setText(author.name);
            } else if (author.email != null) {
                holder.user.setText(author.email);
            }
        }

        String message = commit.shortMessage();
        if (commit.commit != null && commit.commit.shortMessage() != null) {
            message = commit.commit.shortMessage();
        }

        if (shortMessage) {
            try {
                holder.title.setText(TextUtils.splitLines(message, 2));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            holder.title.setText(message);
        }
        emojiBitmapLoader.parseTextView(holder.title);

        if (commit.sha != null) {
            holder.sha.setText(commit.shortSha());
        }

        holder.textNums.setText("");

        if (commit.stats != null) {
            String textCommitsStr = null;
            if (commit.stats.additions > 0 && commit.stats.deletions > 0) {
                textCommitsStr = holder.itemView.getContext().getString(R.string.commit_file_add_del, commit.stats.additions, commit.stats.deletions);
                holder.textNums.setVisibility(View.VISIBLE);
            } else if (commit.stats.additions > 0) {
                textCommitsStr = holder.itemView.getContext().getString(R.string.commit_file_add, commit.stats.additions);
                holder.textNums.setVisibility(View.VISIBLE);
            } else if (commit.stats.deletions > 0) {
                textCommitsStr = holder.itemView.getContext().getString(R.string.commit_file_del, commit.stats.deletions);
                holder.textNums.setVisibility(View.VISIBLE);
            } else {
                holder.textNums.setVisibility(View.GONE);
            }

            if (textCommitsStr != null) {
                holder.textNums.setText(Html.fromHtml(textCommitsStr));
            }
        } else {
            holder.textNums.setVisibility(View.GONE);
        }

        if (commit.files != null && commit.files.size() > 0) {
            holder.numFiles.setVisibility(View.VISIBLE);
            holder.numFiles.setText(holder.itemView.getContext().getString(R.string.num_of_files, commit.files.size()));
        } else {
            holder.numFiles.setVisibility(View.GONE);
        }

        holder.comments_count.setText(String.valueOf(commit.comment_count));
        applyIcon(holder.comments_count, Octicons.Icon.oct_comment_discussion);
    }

    private void applyIcon(TextView textView, Octicons.Icon value) {
        IconicsDrawable drawableForks = new IconicsDrawable(textView.getContext(), value);
        drawableForks.sizeRes(R.dimen.textSizeSmall);
        drawableForks.colorRes(R.color.icons);
        textView.setCompoundDrawables(null, null, drawableForks, null);
        int offset = textView.getResources().getDimensionPixelOffset(R.dimen.textSizeSmall);
        textView.setCompoundDrawablePadding(offset);
    }

    @Override
    public long getHeaderId(int i) {
        return getItem(i).days;
    }

    @Override
    public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup viewGroup) {
        return new HeaderViewHolder(getInflater().inflate(R.layout.commit_row_header, viewGroup, false));
    }

    @Override
    public void onBindHeaderViewHolder(HeaderViewHolder headerViewHolder, int i) {
        Commit commit = getItem(i);

        if (commit.commit != null && commit.commit.author != null && commit.commit.author.date != null) {
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            DateTime dt = formatter.parseDateTime(commit.commit.author.date);

            String text = dt.toString("dd MMM yyyy");

            headerViewHolder.tv.setText(text);
        }
    }

    public void setCommitsAdapterListener(CommitsAdapterListener commitsAdapterListener) {
        this.commitsAdapterListener = commitsAdapterListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView user;
        private final TextView sha;
        private final TextView textNums;
        private final TextView numFiles;
        private final TextView comments_count;
        private final ImageView avatar;

        public ViewHolder(final View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.title);
            user = (TextView) itemView.findViewById(R.id.user);
            sha = (TextView) itemView.findViewById(R.id.sha);
            textNums = (TextView) itemView.findViewById(R.id.textNums);
            numFiles = (TextView) itemView.findViewById(R.id.numFiles);
            comments_count = (TextView) itemView.findViewById(R.id.comments_count);
            avatar = (ImageView) itemView.findViewById(R.id.avatarAuthor);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (commitsAdapterListener != null) {
                        Commit commit = getItem(getAdapterPosition());
                        commitsAdapterListener.onCommitClick(commit);
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (commitsAdapterListener != null) {
                        Commit commit = getItem(getAdapterPosition());
                        return commitsAdapterListener.onCommitLongClick(commit);
                    }
                    return true;
                }
            });
        }

        public void copy(String text) {
            ClipboardManager clipboard = (ClipboardManager) itemView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Gitskarios", text);
            clipboard.setPrimaryClip(clip);
        }
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final TextView tv;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            tv = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }

    public interface CommitsAdapterListener {
        void onCommitClick(Commit commit);

        boolean onCommitLongClick(Commit commit);
    }
}

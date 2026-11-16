package com.liskovsoft.smartyoutubetv2.common.oculo;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * 32.74-16 crash (Continue watching, in-place update): the ACTION_REMOVE group was the removed cards' own group,
 * so VideoGroupObjectAdapter.remove() removed each card from the list it was iterating.
 */
public class OculoContinueWatchingRefGroupTest {
    private static Video video(String id) {
        Video video = new Video();
        video.videoId = id;
        return video;
    }

    /** What VideoGroupObjectAdapter.remove() does with the group's videos (minus the RecyclerView part). */
    private static void adapterRemove(VideoGroup group) {
        for (Video video : group.getVideos()) {
            if (video.getGroup() != null) {
                video.getGroup().remove(video);
            }
        }
    }

    @Test
    public void removeGroupDoesNotOwnTheCards() {
        Video a = video("a");
        Video b = video("b");
        Video c = video("c");
        VideoGroup row = VideoGroup.from(new ArrayList<>(Arrays.asList(a, b, c)), null, OculoContinueWatching.ROW_POSITION);

        VideoGroup remove = OculoContinueWatching.createRefGroup(Arrays.asList(a, b), VideoGroup.ACTION_REMOVE);

        assertEquals(OculoContinueWatching.GROUP_ID, remove.getId());
        assertEquals(VideoGroup.ACTION_REMOVE, remove.getAction());
        assertSame(row, a.getGroup()); // still the row's cards

        adapterRemove(remove); // no ConcurrentModificationException

        assertEquals(1, row.getVideos().size());
        assertSame(c, row.getVideos().get(0));
    }

    @Test
    public void cardWithoutGroupIsNotAdopted() {
        Video a = video("a");
        Video b = video("b");
        VideoGroup remove = OculoContinueWatching.createRefGroup(Arrays.asList(a, b), VideoGroup.ACTION_REMOVE);

        adapterRemove(remove);
        assertEquals(2, remove.getVideos().size());
    }

    @Test
    public void ownedGroupReproducesTheCrash() {
        // The 32.74-16 pattern: a group that is its videos' own group
        Video a = video("a");
        Video b = video("b");
        Video c = video("c");
        List<Video> list = new ArrayList<>(Arrays.asList(a, b, c));
        VideoGroup owned = VideoGroup.from(list, null, OculoContinueWatching.ROW_POSITION);
        assertSame(owned, a.getGroup());

        try {
            adapterRemove(owned);
            fail("expected ConcurrentModificationException");
        } catch (ConcurrentModificationException expected) {
            // the bug
        }
    }
}

/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.reddit.url;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.Constants;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.listingcontrollers.PostListingController;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;

import java.util.ArrayList;
import java.util.List;

public class SubredditPostListURL extends PostListingURL {

	public static SubredditPostListURL getFrontPage() {
		return new SubredditPostListURL(Type.FRONTPAGE, null, null, null, null, null);
	}

	public static SubredditPostListURL getAll() {
		return new SubredditPostListURL(Type.ALL, null, null, null, null, null);
	}

	public static RedditURLParser.RedditURL getSubreddit(String subreddit) throws RedditSubreddit.InvalidSubredditNameException {

		Uri.Builder builder = new Uri.Builder();
		builder.scheme(Constants.Reddit.getScheme()).authority(Constants.Reddit.getDomain());

		builder.encodedPath("/r/");
		builder.appendPath(RedditSubreddit.stripRPrefix(subreddit));

		return RedditURLParser.parse(builder.build());
	}

	public enum Type {
		FRONTPAGE, ALL, SUBREDDIT, SUBREDDIT_COMBINATION, ALL_SUBTRACTION
	}

	public final Type type;
	public final String subreddit;

	@Nullable public final PostListingController.Sort order;
	@Nullable public final Integer limit;
	@Nullable public final String before, after;

	private SubredditPostListURL(
			Type type,
			String subreddit,
			@Nullable PostListingController.Sort order,
			@Nullable Integer limit,
			@Nullable String before,
			@Nullable String after) {

		this.type = type;
		this.subreddit = subreddit;
		this.order = order;
		this.limit = limit;
		this.before = before;
		this.after = after;
	}

	public SubredditPostListURL after(String newAfter) {
		return new SubredditPostListURL(type, subreddit, order, limit, before, newAfter);
	}

	public SubredditPostListURL limit(Integer newLimit) {
		return new SubredditPostListURL(type, subreddit, order, newLimit, before, after);
	}

	public SubredditPostListURL sort(PostListingController.Sort newOrder) {
		return new SubredditPostListURL(type, subreddit, newOrder, limit, before, after);
	}

	public PostListingController.Sort getOrder() {
		return order;
	}

	@Nullable
	private static PostListingController.Sort getOrder(@Nullable String sort, @Nullable String t) {

		if(sort == null) {
			return null;
		}

		sort = General.asciiLowercase(sort);
		t = t != null ? General.asciiLowercase(t) : null;

		if(sort.equals("hot")) {
			return PostListingController.Sort.HOT;
		} else if(sort.equals("new")) {
			return PostListingController.Sort.NEW;
		} else if(sort.equals("controversial")) {
			return PostListingController.Sort.CONTROVERSIAL;
		} else if(sort.equals("rising")) {
			return PostListingController.Sort.RISING;
		} else if(sort.equals("top")) {

			if(t == null)				return PostListingController.Sort.TOP_ALL;
			else if(t.equals("all"))	return PostListingController.Sort.TOP_ALL;
			else if(t.equals("hour"))	return PostListingController.Sort.TOP_HOUR;
			else if(t.equals("day"))	return PostListingController.Sort.TOP_DAY;
			else if(t.equals("week"))	return PostListingController.Sort.TOP_WEEK;
			else if(t.equals("month"))	return PostListingController.Sort.TOP_MONTH;
			else if(t.equals("year"))	return PostListingController.Sort.TOP_YEAR;
			else						return PostListingController.Sort.TOP_ALL;

		} else {
			return null;
		}
	}


	@Override
	public Uri generateJsonUri() {

		Uri.Builder builder = new Uri.Builder();
		builder.scheme(Constants.Reddit.getScheme()).authority(Constants.Reddit.getDomain());

		switch(type) {

			case FRONTPAGE:
				builder.encodedPath("/");
				break;

			case ALL:
				builder.encodedPath("/r/all");
				break;

			case SUBREDDIT:
			case SUBREDDIT_COMBINATION:
			case ALL_SUBTRACTION:
				builder.encodedPath("/r/");
				builder.appendPath(subreddit);
				break;
		}

		if(order != null) {
			switch(order) {

				case HOT:
				case NEW:
				case RISING:
				case CONTROVERSIAL:
					builder.appendEncodedPath(General.asciiLowercase(order.name()));
					break;

				case TOP_HOUR:
				case TOP_DAY:
				case TOP_WEEK:
				case TOP_MONTH:
				case TOP_YEAR:
				case TOP_ALL:
					builder.appendEncodedPath("top");
					builder.appendQueryParameter("t", General.asciiLowercase(order.name().split("_")[1]));
					break;
			}
		}

		if(before != null) {
			builder.appendQueryParameter("before", before);
		}

		if(after != null) {
			builder.appendQueryParameter("after", after);
		}

		if(limit != null) {
			builder.appendQueryParameter("limit", String.valueOf(limit));
		}

		builder.appendEncodedPath(".json");

		return builder.build();
	}

	@Override
	public @RedditURLParser.PathType int pathType() {
		return RedditURLParser.SUBREDDIT_POST_LISTING_URL;
	}

	public static SubredditPostListURL parse(final Uri uri) {

		Integer limit = null;
		String before = null, after = null;

		for(final String parameterKey : General.getUriQueryParameterNames(uri)) {

			if(parameterKey.equalsIgnoreCase("after")) {
				after = uri.getQueryParameter(parameterKey);

			} else if(parameterKey.equalsIgnoreCase("before")) {
				before = uri.getQueryParameter(parameterKey);

			} else if(parameterKey.equalsIgnoreCase("limit")) {
				try {
					limit = Integer.parseInt(uri.getQueryParameter(parameterKey));
				} catch(Throwable ignored) {}

			}
		}

		final String[] pathSegments;
		{
			final List<String> pathSegmentsList = uri.getPathSegments();

			final ArrayList<String> pathSegmentsFiltered = new ArrayList<>(pathSegmentsList.size());
			for(String segment : pathSegmentsList) {

				while(General.asciiLowercase(segment).endsWith(".json") || General.asciiLowercase(segment).endsWith(".xml")) {
					segment = segment.substring(0, segment.lastIndexOf('.'));
				}

				if(segment.length() > 0) {
					pathSegmentsFiltered.add(segment);
				}
			}

			pathSegments = pathSegmentsFiltered.toArray(new String[pathSegmentsFiltered.size()]);
		}

		final PostListingController.Sort order;
		if(pathSegments.length > 0) {
			order = getOrder(pathSegments[pathSegments.length - 1], uri.getQueryParameter("t"));
		} else {
			order = null;
		}

		switch(pathSegments.length) {
			case 0:
				return new SubredditPostListURL(Type.FRONTPAGE, null, null, limit, before, after);

			case 1: {
				if(order != null) {
					return new SubredditPostListURL(Type.FRONTPAGE, null, order, limit, before, after);
				} else {
					return null;
				}
			}

			case 2:
			case 3: {

				if(!pathSegments[0].equals("r")) return null;

				final String subreddit = pathSegments[1];

				if(subreddit.equals("all")) {

					if(pathSegments.length == 2) {
						return new SubredditPostListURL(Type.ALL, null, null, limit, before, after);

					} else if(order != null) {
						return new SubredditPostListURL(Type.ALL, null, order, limit, before, after);

					} else {
						return null;
					}

				} else if(subreddit.matches("all(\\-[\\w\\.]+)+")) {

					if(pathSegments.length == 2) {
						return new SubredditPostListURL(Type.ALL_SUBTRACTION, subreddit, null, limit, before, after);

					} else if(order != null) {
						return new SubredditPostListURL(Type.ALL_SUBTRACTION, subreddit, order, limit, before, after);

					} else {
						return null;
					}

				} else if(subreddit.matches("\\w+(\\+[\\w\\.]+)+")) {

					if(pathSegments.length == 2) {
						return new SubredditPostListURL(Type.SUBREDDIT_COMBINATION, subreddit, null, limit, before, after);

					} else if(order != null) {
						return new SubredditPostListURL(Type.SUBREDDIT_COMBINATION, subreddit, order, limit, before, after);

					} else {
						return null;
					}

				} else if(subreddit.matches("[\\w\\.]+")) {

					if(pathSegments.length == 2) {
						return new SubredditPostListURL(Type.SUBREDDIT, subreddit, null, limit, before, after);

					} else if(order != null) {
						return new SubredditPostListURL(Type.SUBREDDIT, subreddit, order, limit, before, after);

					} else {
						return null;
					}

				} else {
					return null;
				}
			}

			default:
				return null;
		}
	}

	@Override
	public String humanReadablePath() {

		String path = super.humanReadablePath();

		if(order == null) {
			return path;
		}

		switch(order) {
			case TOP_HOUR:
			case TOP_DAY:
			case TOP_WEEK:
			case TOP_MONTH:
			case TOP_YEAR:
			case TOP_ALL:
				return path + "?t=" + General.asciiLowercase(order.name().split("_")[1]);

			default:
				return path;
		}
	}

	@Override
	public String humanReadableName(Context context, boolean shorter) {

		switch(type) {

			case FRONTPAGE:
				return context.getString(R.string.mainmenu_frontpage);

			case ALL:
				return context.getString(R.string.mainmenu_all);

			case SUBREDDIT:
				try {
					return RedditSubreddit.getCanonicalName(subreddit);
				} catch(RedditSubreddit.InvalidSubredditNameException e) {
					return subreddit;
				}

			case SUBREDDIT_COMBINATION:
			case ALL_SUBTRACTION:
				return subreddit;

			default:
				return super.humanReadableName(context, shorter);
		}
	}
}

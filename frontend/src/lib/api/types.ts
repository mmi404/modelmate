/**
 * Hand-mirrored request/response shapes from the backend
 * (see v2/docs/API.md and the DTOs under backend/src/main/java/com/modelmate).
 * Keep in sync manually — there is no shared schema generator yet.
 */

export type Role = "USER" | "ADMIN";

export interface UserDto {
  id: number;
  firstName: string;
  lastName: string;
  email: string | null;
  role: Role;
  avatarUrl: string | null;
  bio: string | null;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: UserDto;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string> | null;
}

export interface CategoryDto {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  applications: string[];
  modelCount: number;
}

export interface CategoryRef {
  slug: string;
  name: string;
}

export interface RatingSummary {
  overall: string | null; // BigDecimal serialises as a string
  accuracy: number | null;
  speed: number | null;
  cost: number | null;
  easeOfUse: number | null;
  reliability: number | null;
  reviewCount: number;
}

export interface ModelCardDto {
  id: number;
  name: string;
  slug: string;
  creator: string | null;
  category: CategoryRef;
  description: string | null;
  ratings: RatingSummary;
}

export interface UserRef {
  id: number;
  name: string;
}

export interface ModelDetailDto {
  id: number;
  name: string;
  slug: string;
  creator: string | null;
  description: string | null;
  websiteUrl: string | null;
  logoUrl: string | null;
  category: CategoryRef;
  submitter: UserRef;
  createdAt: string;
  ratings: RatingSummary;
  problemCount: number;
}

export type ReviewType = "REVIEW" | "PROBLEM";
export type Severity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface Ratings {
  accuracy: number | null;
  speed: number | null;
  cost: number | null;
  easeOfUse: number | null;
  reliability: number | null;
}

export interface ReviewerRef {
  id: number;
  name: string;
  avatarUrl: string | null;
}

export interface ReviewDto {
  id: number;
  type: ReviewType;
  title: string | null;
  content: string;
  ratings: Ratings | null;
  overallRating: string | null;
  severity: Severity | null;
  reviewer: ReviewerRef;
  upvoteCount: number;
  downvoteCount: number;
  myVote: number | null;
  createdAt: string;
}

export interface LeaderboardEntry {
  rank: number;
  topThree: boolean;
  modelId: number;
  name: string;
  slug: string;
  creator: string | null;
  categorySlug: string;
  categoryName: string;
  overall: string | null;
  reviewCount: number;
}

export interface DiscussionDto {
  id: number;
  title: string;
  content: string;
  tags: string[];
  author: ReviewerRef;
  replyCount: number;
  upvoteCount: number;
  downvoteCount: number;
  myVote: number | null;
  createdAt: string;
}

export interface ReplyDto {
  id: number;
  discussionId: number;
  parentReplyId: number | null;
  author: ReviewerRef;
  content: string;
  upvoteCount: number;
  downvoteCount: number;
  myVote: number | null;
  createdAt: string;
}

export interface DiscussionStats {
  activeMembers: number;
  totalDiscussions: number;
  totalReplies: number;
}

export interface TagCountDto {
  tag: string;
  count: number;
}

export type VoteTargetType = "DISCUSSION" | "REPLY" | "REVIEW";

export interface VoteResult {
  upvoteCount: number;
  downvoteCount: number;
  myVote: number | null;
}

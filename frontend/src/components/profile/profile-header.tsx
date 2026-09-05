import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { formatDate } from "@/lib/format";
import type { UserDto } from "@/lib/api/types";

export function ProfileHeader({ user }: { user: UserDto }) {
  const name = `${user.firstName} ${user.lastName}`.trim();
  return (
    <header className="flex items-start gap-4">
      <Avatar className="size-16">
        {user.avatarUrl && <AvatarImage src={user.avatarUrl} alt="" />}
        <AvatarFallback className="text-lg">
          {(user.firstName[0] ?? "") + (user.lastName[0] ?? "")}
        </AvatarFallback>
      </Avatar>
      <div className="min-w-0">
        <div className="flex items-center gap-2">
          <h1 className="text-2xl font-bold tracking-tight">{name}</h1>
          {user.role === "ADMIN" && <Badge>Admin</Badge>}
        </div>
        <p className="text-sm text-muted-foreground">Member since {formatDate(user.createdAt)}</p>
        {user.bio && <p className="mt-2 max-w-prose text-sm">{user.bio}</p>}
      </div>
    </header>
  );
}

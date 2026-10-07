import Image from "next/image";
import { formatRelative } from "@/lib/format";
import type { YoutubeVideo } from "@/lib/youtube";

/** 채널 최신 영상. 쇼츠는 세로 비율 대신 같은 카드에 "Shorts" 표시만 붙인다. */
export function VideoRow({ videos }: { videos: YoutubeVideo[] }) {
  return (
    <div className="-mx-4 flex snap-x scroll-px-4 gap-4 overflow-x-auto px-4 pb-2 sm:mx-0 sm:scroll-px-0 sm:grid sm:grid-cols-4 sm:overflow-visible sm:px-0">
      {videos.map((video) => (
        <a
          key={video.id}
          href={video.url}
          target="_blank"
          rel="noopener noreferrer"
          className="glow-hover group w-[72%] shrink-0 snap-start overflow-hidden rounded-xl border border-border bg-surface sm:w-auto"
        >
          <div className="relative aspect-video bg-surface-2">
            <Image src={video.thumbnailUrl} alt="" fill sizes="(min-width: 640px) 25vw, 72vw" className="object-cover" />
            {video.shorts && (
              <span className="absolute top-2 left-2 rounded bg-danger px-1.5 py-0.5 font-pixel text-[11px] text-white">
                Shorts
              </span>
            )}
          </div>
          <div className="space-y-1 p-3">
            <p className="line-clamp-2 text-sm leading-snug font-medium group-hover:text-accent">{video.title}</p>
            <p className="text-xs text-muted">{formatRelative(video.publishedAt)}</p>
          </div>
        </a>
      ))}
    </div>
  );
}

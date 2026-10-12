import "server-only";

/**
 * 유튜브 oEmbed는 비공개·삭제·퍼가기 금지 영상이면 오류를 준다. 그래서 ok=false면 사이트에 붙일 수 없는 영상으로 본다.
 */
/** 유튜브 oEmbed(키 불필요)로 영상 제목과 채널 이름을 가져온다. 실패하면 빈 값 */
export async function youtubeInfo(url: string): Promise<{ title: string; author: string; ok: boolean }> {
  try {
    // 영상 ID만 넣은 경우도 oEmbed가 알아보도록 주소로 바꾼다
    const target = /^[A-Za-z0-9_-]{11}$/.test(url) ? `https://www.youtube.com/watch?v=${url}` : url;
    const res = await fetch(`https://www.youtube.com/oembed?format=json&url=${encodeURIComponent(target)}`, {
      cache: "no-store",
      signal: AbortSignal.timeout(5000),
    });
    if (!res.ok) return { title: "", author: "", ok: false };
    const data: unknown = await res.json();
    if (typeof data !== "object" || data === null) return { title: "", author: "", ok: false };
    const title = "title" in data && typeof data.title === "string" ? data.title.slice(0, 200) : "";
    const author = "author_name" in data && typeof data.author_name === "string" ? data.author_name.slice(0, 100) : "";
    return { title, author, ok: true };
  } catch {
    return { title: "", author: "", ok: false };
  }
}

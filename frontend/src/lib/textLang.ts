/**
 * 큐레이터가 적은 글(엔딩·업적 공략)은 영어 화면에서도 번역이 없으면 한국어다.
 * 한글이 섞여 있으면 "ko"를 돌려줘 화면 읽기 프로그램이 한국어로 읽게 하고, 아니면 페이지 언어를 따른다.
 */
export function textLang(...texts: (string | null | undefined)[]): "ko" | undefined {
  return texts.some((text) => text && /[\u3131-\u318E\uAC00-\uD7A3]/.test(text)) ? "ko" : undefined;
}

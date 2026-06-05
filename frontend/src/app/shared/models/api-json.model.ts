export type ApiJson =
  | string
  | number
  | boolean
  | null
  | ApiJson[]
  | { [key: string]: ApiJson };

export type ApiJsonObject = { [key: string]: ApiJson };

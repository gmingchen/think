import { IMAGE_SUFFIX_LIST, VIDEO_SUFFIX_LIST, WORD_SUFFIX_LIST, EXCEL_SUFFIX_LIST, PPT_SUFFIX_LIST, PDF_SUFFIX_LIST } from '../../constants/index'
// 文件类型
export const FILE_TYPE_ENUM = {
  IMAGE: 'image', // 图片
  VIDEO: 'video', // 视频
  WORD: 'word', // word
  EXCEL: 'excel', // excel
  PPT: 'ppt', // ppt
  PDF: 'pdf', // pdf
}
/**
 * 文件类型列表
 */
export const FILE_TYPE_LIST = [
  { label: FILE_TYPE_ENUM.IMAGE, value: IMAGE_SUFFIX_LIST },
  { label: FILE_TYPE_ENUM.VIDEO, value: VIDEO_SUFFIX_LIST },
  { label: FILE_TYPE_ENUM.WORD, value: WORD_SUFFIX_LIST },
  { label: FILE_TYPE_ENUM.EXCEL, value: EXCEL_SUFFIX_LIST },
  { label: FILE_TYPE_ENUM.PPT, value: PPT_SUFFIX_LIST },
  { label: FILE_TYPE_ENUM.PDF, value: PDF_SUFFIX_LIST },
]

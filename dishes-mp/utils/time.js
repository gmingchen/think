import dayjs from 'dayjs'

/**
 * 格式化时间
 * @param {*} time 
 */
export const timeFormat = (time) => {
  time = dayjs(time).toDate();
  const now = new Date();
  // 计算时间差（毫秒）
  const diff = now - time;
  // 计算时间差的不同单位
  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days >= 2) {
    const year = time.getFullYear();
    const month = String(time.getMonth() + 1);
    const day = String(time.getDate()).padStart(2, '0');
    return `${year}年${+month}月${+day}日`;
  } if (days >= 1) {
    const hour = String(time.getHours()).padStart(2, '0');
    const minute = String(time.getMinutes()).padStart(2, '0');
    return `昨天 ${hour}:${minute}`;
  } else {
    const hour = String(time.getHours()).padStart(2, '0');
    const minute = String(time.getMinutes()).padStart(2, '0');
    return `${hour}:${minute}`;
  }
}
/**
 * 两个日期相差计算
 * @param {*} start 
 * @param {*} end 
 */
export const timeDiff = (start, end) => {
  start = dayjs(start)
  end = dayjs(end)
  if (start.isAfter(end)) {
    const temp = start
    start = end
    end = temp
  }

  const diffDays = end.diff(start, 'day')
  start = start.add(diffDays, 'day')

  const diffHours = end.diff(start, 'hour')
  start = start.add(diffHours, 'hour')

  const diffMinutes = end.diff(start, 'minute')
  start = start.add(diffMinutes, 'minute')

  let result = '';
  if (diffDays > 0) {
    result += `${diffDays}天`;
  }
  if (diffHours > 0) {
    result += `${diffHours}小时`;
  }
  if (diffMinutes > 0) {
    result += `${diffMinutes}分钟`;
  }
  return result
}
